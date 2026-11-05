/**
 * WhatsApp Baileys Client & SBI Statement State Machine
 */

import makeWASocket, {
  useMultiFileAuthState,
  DisconnectReason,
  fetchLatestBaileysVersion,
  normalizeMessageContent,
  generateWAMessageFromContent,
  proto
} from '@whiskeysockets/baileys';
import QRCode from 'qrcode';
import pino from 'pino';
import fs from 'fs';
import path from 'path';

const TRIGGER_REGEX = /please choose from any of the options|select an option|choose one of the options/i;
const OPTION_REGEX = /get\s*mini\s*statement/i;
const INTERMEDIATE_WAIT_REGEX = /please wait while we fetch your mini statement|please wait while we fetch/i;

export class WhatsAppClient {
  constructor(options = {}) {
    this.authDir = options.authDir || process.env.AUTH_DIR || './auth';
    this.targetNumber = options.targetNumber || process.env.SBI_NUMBER || '919022690226';
    this.onStatementReceived = options.onStatementReceived || (() => {});
    this.onStatusChange = options.onStatusChange || (() => {});
    this.onLog = options.onLog || console.log;

    this.sock = null;
    this.status = 'disconnected'; // disconnected, connecting, qr_ready, pair_code_ready, connected, reconnecting
    this.qrCodeDataUrl = null;
    this.pairingCode = null;
    this.connectedPhoneNumber = null;

    // Active cycle state
    this.activeJob = null;
    this.jobTimeout = null;
  }

  log(msg) {
    this.onLog(`[WhatsApp] ${msg}`);
  }

  async start() {
    try {
      if (!fs.existsSync(this.authDir)) {
        fs.mkdirSync(this.authDir, { recursive: true });
      }

      this.status = 'connecting';
      this.onStatusChange(this.getStatus());

      const { state: authState, saveCreds } = await useMultiFileAuthState(this.authDir);
      const { version } = await fetchLatestBaileysVersion().catch(() => ({ version: [2, 3000, 1015901307] }));

      this.sock = makeWASocket({
        version,
        auth: authState,
        logger: pino({ level: 'silent' }),
        printQRInTerminal: false,
        browser: ['FinPulse SBI Statement Guardian', 'Chrome', '124.0.0.0'],
        markOnlineOnConnect: false,
        connectTimeoutMs: 60000,
        defaultQueryTimeoutMs: 60000
      });

      this.sock.ev.on('creds.update', saveCreds);

      this.sock.ev.on('connection.update', async ({ connection, lastDisconnect, qr }) => {
        if (qr) {
          try {
            this.qrCodeDataUrl = await QRCode.toDataURL(qr, { margin: 1, width: 280 });
            this.status = 'qr_ready';
            this.log('QR Code ready for scanning');
            this.onStatusChange(this.getStatus());
          } catch (e) {
            this.log(`QR generation error: ${e.message}`);
          }
        }

        if (connection === 'open') {
          this.status = 'connected';
          this.qrCodeDataUrl = null;
          this.pairingCode = null;
          const userJid = this.sock.user?.id || '';
          this.connectedPhoneNumber = userJid.split(':')[0].split('@')[0];
          this.log(`Connected successfully: +${this.connectedPhoneNumber}`);
          this.onStatusChange(this.getStatus());
        }

        if (connection === 'close') {
          const statusCode = lastDisconnect?.error?.output?.statusCode;
          this.status = 'disconnected';
          this.connectedPhoneNumber = null;

          if (statusCode === DisconnectReason.loggedOut) {
            this.log('Session logged out by user. Clearing auth cache.');
            try {
              fs.rmSync(this.authDir, { recursive: true, force: true });
            } catch (_) {}
            this.onStatusChange(this.getStatus());
          } else {
            this.log(`Connection closed (code: ${statusCode}). Reconnecting in 3s...`);
            this.status = 'reconnecting';
            this.onStatusChange(this.getStatus());
            setTimeout(() => this.start(), 3000);
          }
        }
      });

      this.sock.ev.on('messages.upsert', ({ messages }) => {
        for (const msg of messages) {
          this.handleIncomingMessage(msg).catch(e => this.log(`Error handling message: ${e.message}`));
        }
      });

    } catch (err) {
      this.status = 'disconnected';
      this.log(`Start error: ${err.message}`);
      this.onStatusChange(this.getStatus());
      setTimeout(() => this.start(), 5000);
    }
  }

  async requestPairingCode(phoneNumber) {
    if (!this.sock) {
      throw new Error('WhatsApp service not initialized');
    }
    const cleanPhone = String(phoneNumber || '').replace(/\D/g, '');
    if (!cleanPhone || cleanPhone.length < 10) {
      throw new Error('Valid phone number with country code is required');
    }

    // Wait until socket is ready to accept pairing code
    for (let i = 0; i < 20 && !this.qrCodeDataUrl && this.status !== 'qr_ready'; i++) {
      await new Promise(r => setTimeout(r, 500));
    }

    try {
      const code = await this.sock.requestPairingCode(cleanPhone);
      this.pairingCode = code;
      this.status = 'pair_code_ready';
      this.log(`Pairing code generated for +${cleanPhone}: ${code}`);
      this.onStatusChange(this.getStatus());
      return code;
    } catch (err) {
      this.log(`Failed to request pairing code: ${err.message}`);
      throw err;
    }
  }

  async logout() {
    this.log('Logging out WhatsApp session...');
    try {
      if (this.sock) {
        await this.sock.logout();
      }
    } catch (_) {}
    try {
      fs.rmSync(this.authDir, { recursive: true, force: true });
    } catch (_) {}
    this.status = 'disconnected';
    this.sock = null;
    this.qrCodeDataUrl = null;
    this.pairingCode = null;
    this.connectedPhoneNumber = null;
    this.onStatusChange(this.getStatus());
  }

  /**
   * Triggers the 5-step SBI Mini Statement sequence.
   */
  async triggerMiniStatementCycle(targetNumber = null) {
    if (this.status !== 'connected' || !this.sock) {
      throw new Error('WhatsApp is not connected. Please link WhatsApp first.');
    }

    if (this.activeJob && ['sending_hi', 'waiting_menu', 'waiting_statement'].includes(this.activeJob.stage)) {
      throw new Error('A statement fetch cycle is already in progress. Please wait.');
    }

    const target = (targetNumber || this.targetNumber).replace(/\D/g, '');
    const jid = `${target}@s.whatsapp.net`;

    this.activeJob = {
      target,
      jid,
      stage: 'sending_hi',
      startedAt: Date.now()
    };

    this.log(`Step 1: Sending "Hi" to SBI target +${target}...`);

    // Timeout safety: if SBI doesn't reply within 60 seconds, reset job
    clearTimeout(this.jobTimeout);
    this.jobTimeout = setTimeout(() => {
      if (this.activeJob && this.activeJob.stage !== 'completed') {
        this.log('Mini statement fetch timed out waiting for SBI response.');
        this.activeJob.stage = 'timeout';
        this.onStatusChange(this.getStatus());
      }
    }, 90000);

    try {
      await this.sock.sendMessage(jid, { text: 'Hi' });
      this.activeJob.stage = 'waiting_menu';
      this.log('Message "Hi" delivered. Awaiting SBI interactive menu...');
      this.onStatusChange(this.getStatus());
      return { success: true, message: 'Statement request initiated' };
    } catch (err) {
      this.activeJob.stage = 'error';
      this.log(`Failed to send "Hi": ${err.message}`);
      this.onStatusChange(this.getStatus());
      throw err;
    }
  }

  async handleIncomingMessage(msg) {
    if (!msg.message || msg.key.fromMe || !this.activeJob) return;

    const jid = msg.key.remoteJid;
    // Accept messages from target JID or new-style lid
    if (!jid.endsWith('@lid') && jid.split('@')[0] !== this.activeJob.target) {
      return;
    }

    const content = normalizeMessageContent(msg.message);
    const text = this.extractTextFromContent(content);
    this.log(`Incoming SBI Message: ${text.slice(0, 100)}...`);

    // STEP 2 & 3: Waiting for menu and selecting "Get Mini Statement📑"
    if (this.activeJob.stage === 'waiting_menu') {
      if (TRIGGER_REGEX.test(text) || OPTION_REGEX.test(text)) {
        this.log('Step 2: SBI Menu detected. Searching for "Get Mini Statement" option...');
        const option = this.findMiniStatementOption(content);

        try {
          if (option) {
            this.log(`Step 3: Clicking button: "${option.title}" (${option.kind})`);
            await this.clickOption(jid, msg, content, option);
          } else {
            this.log('Step 3 (Fallback): Option button not detected, sending text reply "Get Mini Statement"');
            await this.sock.sendMessage(jid, { text: 'Get Mini Statement' });
          }
          this.activeJob.stage = 'waiting_statement';
          this.onStatusChange(this.getStatus());
        } catch (e) {
          this.log(`Step 3 click failed: ${e.message}`);
          this.activeJob.stage = 'error';
        }
      }
      return;
    }

    // STEP 4 & 5: Waiting for intermediate message vs actual mini statement
    if (this.activeJob.stage === 'waiting_statement') {
      // Step 4: Intermediate wait message
      if (INTERMEDIATE_WAIT_REGEX.test(text)) {
        this.log('Step 4: Intermediate message detected: "Please wait while we fetch your mini statement.... ⏳" - continuing to wait for final statement.');
        return;
      }

      // Step 5: Actual mini statement text received
      this.log('Step 5: Statement payload received! Handing off to statement parser...');
      this.activeJob.stage = 'completed';
      clearTimeout(this.jobTimeout);
      this.onStatusChange(this.getStatus());

      // Callback to parse and synchronize with cPanel MySQL
      try {
        await this.onStatementReceived(text, this.activeJob.target);
      } catch (e) {
        this.log(`Error in statement processing callback: ${e.message}`);
      }
    }
  }

  extractTextFromContent(c) {
    if (!c) return '';
    const parts = [
      c.conversation,
      c.extendedTextMessage?.text,
      c.listMessage?.title,
      c.listMessage?.description,
      c.buttonsMessage?.contentText,
      c.interactiveMessage?.header?.title,
      c.interactiveMessage?.body?.text,
      c.templateMessage?.hydratedTemplate?.hydratedContentText,
      c.imageMessage?.caption,
      c.listResponseMessage?.title
    ];
    return parts.filter(Boolean).join('\n');
  }

  findMiniStatementOption(c) {
    // 1. List sections
    for (const s of c.listMessage?.sections || []) {
      for (const r of s.rows || []) {
        if (OPTION_REGEX.test(r.title || '')) {
          return { kind: 'list', id: r.rowId, title: r.title };
        }
      }
    }
    // 2. Buttons
    for (const b of c.buttonsMessage?.buttons || []) {
      const text = b.buttonText?.displayText || '';
      if (OPTION_REGEX.test(text)) {
        return { kind: 'button', id: b.buttonId, title: text };
      }
    }
    // 3. Hydrated template buttons
    for (const b of c.templateMessage?.hydratedTemplate?.hydratedButtons || []) {
      const text = b.quickReplyButton?.displayText || '';
      if (OPTION_REGEX.test(text)) {
        return { kind: 'template', id: b.quickReplyButton.id, title: text };
      }
    }
    // 4. Native interactive flow buttons
    for (const b of c.interactiveMessage?.nativeFlowMessage?.buttons || []) {
      let params = {};
      try { params = JSON.parse(b.buttonParamsJson || '{}'); } catch {}
      if (b.name === 'single_select') {
        for (const s of params.sections || []) {
          for (const r of s.rows || []) {
            if (OPTION_REGEX.test(r.title || '')) {
              return { kind: 'native', name: 'single_select', id: r.id, title: r.title };
            }
          }
        }
      }
      if (OPTION_REGEX.test(params.display_text || '')) {
        return { kind: 'native', name: 'quick_reply', id: params.id, title: params.display_text };
      }
    }
    return null;
  }

  async clickOption(jid, msg, c, opt) {
    const ctx = {
      stanzaId: msg.key.id,
      participant: msg.key.participant || msg.key.remoteJid,
      quotedMessage: msg.message
    };

    let content;
    if (opt.kind === 'list') {
      content = {
        listResponseMessage: {
          title: opt.title,
          listType: 1,
          singleSelectReply: { selectedRowId: opt.id },
          contextInfo: ctx
        }
      };
    } else if (opt.kind === 'button') {
      content = {
        buttonsResponseMessage: {
          selectedButtonId: opt.id,
          selectedDisplayText: opt.title,
          type: 1,
          contextInfo: ctx
        }
      };
    } else if (opt.kind === 'template') {
      content = {
        templateButtonReplyMessage: {
          selectedId: opt.id,
          selectedDisplayText: opt.title,
          selectedIndex: 0,
          contextInfo: ctx
        }
      };
    } else {
      content = {
        interactiveResponseMessage: {
          body: { text: opt.title, format: 0 },
          nativeFlowResponseMessage: {
            name: opt.name,
            paramsJson: JSON.stringify({ id: opt.id, display_text: opt.title }),
            version: 3
          },
          contextInfo: ctx
        }
      };
    }

    const m = generateWAMessageFromContent(jid, proto.Message.fromObject(content), { userJid: this.sock.user.id });
    await this.sock.relayMessage(jid, m.message, { messageId: m.key.id });
  }

  getStatus() {
    return {
      status: this.status,
      connected: this.status === 'connected',
      phoneNumber: this.connectedPhoneNumber,
      qr: this.qrCodeDataUrl,
      pairCode: this.pairingCode,
      targetNumber: this.targetNumber,
      activeJob: this.activeJob ? {
        stage: this.activeJob.stage,
        startedAt: this.activeJob.startedAt
      } : null
    };
  }
}
