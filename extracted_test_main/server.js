import express from 'express';
import QRCode from 'qrcode';
import pino from 'pino';
import fs from 'fs';
import makeWASocket, {
  useMultiFileAuthState, DisconnectReason, fetchLatestBaileysVersion,
  normalizeMessageContent, generateWAMessageFromContent, proto
} from '@whiskeysockets/baileys';

const PORT = process.env.PORT || 3000;
const AUTH_DIR = process.env.AUTH_DIR || './auth';   // Railway: volume mount karke /data set karo
const APP_PASSWORD = process.env.APP_PASSWORD || '';
const TRIGGER = /please choose from any of the options/i;
const OPTION = /get\s*mini\s*statement/i;
const WAIT = /please wait while we fetch/i;
const JOB_FILE = AUTH_DIR + '/job.json';

const state = { status: 'connecting', qr: null, pairCode: null, me: null, job: null, results: [], logs: [] };
let sock = null, timer = null;
const clients = new Set();

const log = (m) => { state.logs.push({ t: new Date().toISOString(), m }); state.logs = state.logs.slice(-60); push(); };
const push = () => { const d = `data: ${JSON.stringify(state)}\n\n`; clients.forEach(c => c.write(d)); };

async function start() {
  const { state: auth, saveCreds } = await useMultiFileAuthState(AUTH_DIR);
  const { version } = await fetchLatestBaileysVersion();
  sock = makeWASocket({ version, auth, logger: pino({ level: 'silent' }), browser: ['Statement Tool', 'Chrome', '1.0'], markOnlineOnConnect: false });
  sock.ev.on('creds.update', saveCreds);

  sock.ev.on('connection.update', async ({ connection, lastDisconnect, qr }) => {
    if (qr) { state.qr = await QRCode.toDataURL(qr, { margin: 1, width: 280 }); state.status = 'qr'; }
    if (connection === 'open') {
      state.status = 'open'; state.qr = null; state.pairCode = null;
      state.me = sock.user?.id?.split(':')[0].split('@')[0]; log('WhatsApp connected: +' + state.me);
      if (!state.job && fs.existsSync(JOB_FILE)) { try { state.job = { ...JSON.parse(fs.readFileSync(JOB_FILE, 'utf8')), runs: 0 }; log('Saved job resumed'); runCycle(); } catch {} }
    }
    if (connection === 'close') {
      const code = lastDisconnect?.error?.output?.statusCode;
      state.status = 'closed'; state.me = null;
      if (code === DisconnectReason.loggedOut) { fs.rmSync(AUTH_DIR, { recursive: true, force: true }); log('Logged out'); }
      else log('Connection closed (' + code + '), reconnecting...');
      setTimeout(start, 2000);
    }
    push();
  });

  sock.ev.on('messages.upsert', ({ messages }) => messages.forEach(onMessage));
}

// ---------- message helpers ----------
function textOf(c) {
  if (!c) return '';
  const parts = [c.conversation, c.extendedTextMessage?.text, c.listMessage?.title, c.listMessage?.description,
    c.buttonsMessage?.contentText, c.interactiveMessage?.header?.title, c.interactiveMessage?.body?.text,
    c.templateMessage?.hydratedTemplate?.hydratedContentText, c.imageMessage?.caption, c.listResponseMessage?.title];
  return parts.filter(Boolean).join('\n');
}

// returns {kind, id, title} for the "Get Mini Statement" option, or null
function findOption(c) {
  for (const s of c.listMessage?.sections || []) for (const r of s.rows || [])
    if (OPTION.test(r.title || '')) return { kind: 'list', id: r.rowId, title: r.title };
  for (const b of c.buttonsMessage?.buttons || [])
    if (OPTION.test(b.buttonText?.displayText || '')) return { kind: 'button', id: b.buttonId, title: b.buttonText.displayText };
  for (const b of c.templateMessage?.hydratedTemplate?.hydratedButtons || [])
    if (OPTION.test(b.quickReplyButton?.displayText || '')) return { kind: 'template', id: b.quickReplyButton.id, title: b.quickReplyButton.displayText };
  for (const b of c.interactiveMessage?.nativeFlowMessage?.buttons || []) {
    let p = {}; try { p = JSON.parse(b.buttonParamsJson || '{}'); } catch {}
    if (b.name === 'single_select')
      for (const s of p.sections || []) for (const r of s.rows || [])
        if (OPTION.test(r.title || '')) return { kind: 'native', name: 'single_select', id: r.id, title: r.title };
    if (OPTION.test(p.display_text || '')) return { kind: 'native', name: 'quick_reply', id: p.id, title: p.display_text };
  }
  return null;
}

async function clickOption(jid, msg, c, opt) {
  const ctx = { stanzaId: msg.key.id, participant: msg.key.participant || msg.key.remoteJid, quotedMessage: msg.message };
  let content;
  if (opt.kind === 'list') content = { listResponseMessage: { title: opt.title, listType: 1, singleSelectReply: { selectedRowId: opt.id }, contextInfo: ctx } };
  else if (opt.kind === 'button') content = { buttonsResponseMessage: { selectedButtonId: opt.id, selectedDisplayText: opt.title, type: 1, contextInfo: ctx } };
  else if (opt.kind === 'template') content = { templateButtonReplyMessage: { selectedId: opt.id, selectedDisplayText: opt.title, selectedIndex: 0, contextInfo: ctx } };
  else content = { interactiveResponseMessage: {
    body: { text: opt.title, format: 0 },
    nativeFlowResponseMessage: { name: opt.name, paramsJson: JSON.stringify({ id: opt.id, display_text: opt.title }), version: 3 },
    contextInfo: ctx } };
  const m = generateWAMessageFromContent(jid, proto.Message.fromObject(content), { userJid: sock.user.id });
  await sock.relayMessage(jid, m.message, { messageId: m.key.id });
}

async function onMessage(msg) {
  const job = state.job;
  if (!job || msg.key.fromMe || !msg.message || !['waiting_menu', 'waiting_result'].includes(job.stage)) return;
  const jid = msg.key.remoteJid;
  if (!jid.endsWith('@lid') && jid.split('@')[0] !== job.target) return;   // @lid = new-style id, accept
  const c = normalizeMessageContent(msg.message);
  const text = textOf(c);
  log('Incoming [' + Object.keys(c).join(',') + ']: ' + (text.slice(0, 80) || '(no text)'));

  if (job.stage === 'waiting_menu') {
    if (!TRIGGER.test(text)) return;
    const opt = findOption(c);
    try {
      if (opt) { await clickOption(jid, msg, c, opt); log('Clicked option: ' + opt.title + ' (' + opt.kind + ')'); }
      else { await sock.sendMessage(jid, { text: 'Get Mini Statement' }); log('Option button not found, sent as text'); }
      job.stage = 'waiting_result';
    } catch (e) { job.stage = 'error'; log('Click failed: ' + e.message); }
  } else {
    if (WAIT.test(text) || TRIGGER.test(text)) { log('Interim message ignored, waiting for statement...'); return; }
    state.results.unshift({ at: new Date().toISOString(), run: job.runs, text: text || '(media/unsupported message)' });
    state.results = state.results.slice(0, 20);
    job.stage = 'done'; log('Statement received (run #' + job.runs + ')');
  }
  push();
}

// ---------- API ----------
const app = express();
app.use(express.json());
app.use(express.static('public'));
const guard = (req, res, next) => {
  if (!APP_PASSWORD || (req.get('x-pass') || req.query.p) === APP_PASSWORD) return next();
  res.status(401).json({ error: 'Wrong password' });
};

app.get('/api/events', guard, (req, res) => {
  res.set({ 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache', Connection: 'keep-alive' });
  res.flushHeaders(); res.write(`data: ${JSON.stringify(state)}\n\n`);
  clients.add(res); req.on('close', () => clients.delete(res));
});

app.post('/api/pair', guard, async (req, res) => {
  try {
    const phone = String(req.body.phone || '').replace(/\D/g, '');
    if (!phone) return res.status(400).json({ error: 'Phone number daalo (country code ke saath)' });
    for (let i = 0; i < 15 && !state.qr; i++) await new Promise(r => setTimeout(r, 1000));
    state.pairCode = await sock.requestPairingCode(phone); log('Pairing code generated'); push();
    res.json({ ok: true });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

function saveJob() {
  const { target, text, intervalMin } = state.job;
  fs.mkdirSync(AUTH_DIR, { recursive: true });
  fs.writeFileSync(JOB_FILE, JSON.stringify({ target, text, intervalMin }));
}

// one full chain: send message -> menu -> click -> statement. Repeats every intervalMin.
function runCycle() {
  const job = state.job; if (!job) return;
  clearTimeout(timer);
  const ms = job.intervalMin * 60000;
  job.nextAt = new Date(Date.now() + ms).toISOString();
  timer = setTimeout(runCycle, ms);
  if (state.status !== 'open') { log('WhatsApp offline, run skipped'); return push(); }
  job.runs++; job.stage = 'sending';
  sock.sendMessage(job.target + '@s.whatsapp.net', { text: job.text })
    .then(() => { if (state.job === job) { job.stage = 'waiting_menu'; log('Run #' + job.runs + ': message sent'); } push(); })
    .catch(e => { job.stage = 'error'; log('Send failed: ' + e.message); push(); });
  push();
}

app.post('/api/start', guard, (req, res) => {
  if (state.status !== 'open') return res.status(400).json({ error: 'Pehle WhatsApp link karo' });
  const target = String(req.body.target || '').replace(/\D/g, '');
  const text = String(req.body.text || '').trim();
  const intervalMin = Number(req.body.intervalMin);
  if (!target || !text || !(intervalMin >= 1)) return res.status(400).json({ error: 'Number, message aur interval (min 1 minute) sahi daalo' });
  state.job = { target, text, intervalMin, runs: 0, stage: 'sending', nextAt: null };
  saveJob(); log(`Started: +${target}, every ${intervalMin} min`);
  runCycle(); res.json({ ok: true });
});

app.post('/api/stop', guard, (_req, res) => {
  clearTimeout(timer); state.job = null; fs.rmSync(JOB_FILE, { force: true }); log('Stopped'); push(); res.json({ ok: true });
});

app.post('/api/logout', guard, async (_req, res) => { try { await sock.logout(); } catch {} res.json({ ok: true }); });

app.listen(PORT, () => console.log('Running on ' + PORT));
start();
