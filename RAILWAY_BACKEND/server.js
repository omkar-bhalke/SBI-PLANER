/**
 * FinPulse Railway Backend
 * WhatsApp Worker, Automation Scheduler & SBI Mini Statement Service
 */

import express from 'express';
import pino from 'pino';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

import { WhatsAppClient } from './src/whatsappClient.js';
import { CpanelClient } from './src/cpanelClient.js';
import { AutomationScheduler } from './src/automationScheduler.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = process.env.PORT || 3000;
const AUTH_DIR = process.env.AUTH_DIR || (fs.existsSync('/data') ? '/data' : './auth');
const APP_PASSWORD = process.env.APP_PASSWORD || '';
const CPANEL_API_URL = process.env.CPANEL_API_URL || 'https://radhewins.buzz';
const CPANEL_API_SECRET = process.env.CPANEL_API_SECRET || 'finpulse_internal_railway_secret_2026';
const SBI_NUMBER = process.env.SBI_NUMBER || '919022690226';

console.log('----------------------------------------------------');
console.log('🚀 FinPulse Railway WhatsApp Statement Service');
console.log(`• Port: ${PORT}`);
console.log(`• Storage Dir (Volume): ${AUTH_DIR}`);
console.log(`• cPanel API URL: ${CPANEL_API_URL}`);
console.log(`• Default SBI Number: ${SBI_NUMBER}`);
console.log('----------------------------------------------------');

const logs = [];
const clients = new Set();

function log(msg) {
  const line = { t: new Date().toISOString(), m: msg };
  console.log(`[${line.t}] ${msg}`);
  logs.push(line);
  if (logs.length > 80) logs.shift();
  pushSSE();
}

// 1. Initialize Clients
const cpanelClient = new CpanelClient(CPANEL_API_URL, CPANEL_API_SECRET);

let scheduler = null;

const whatsappClient = new WhatsAppClient({
  authDir: AUTH_DIR,
  targetNumber: SBI_NUMBER,
  onLog: log,
  onStatusChange: (status) => {
    pushSSE();
  },
  onStatementReceived: async (rawText, targetNumber) => {
    if (scheduler) {
      await scheduler.processIncomingStatement(rawText, targetNumber);
    }
  }
});

scheduler = new AutomationScheduler({
  authDir: AUTH_DIR,
  whatsappClient,
  cpanelClient,
  onLog: log,
  onStateChange: () => {
    pushSSE();
  }
});

function getCombinedState() {
  return {
    whatsapp: whatsappClient.getStatus(),
    scheduler: scheduler.getState(),
    logs: logs.slice(-40)
  };
}

function pushSSE() {
  const payload = `data: ${JSON.stringify(getCombinedState())}\n\n`;
  for (const client of clients) {
    try {
      client.write(payload);
    } catch (_) {
      clients.delete(client);
    }
  }
}

// 2. Setup Express App
const app = express();
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// CORS
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.header('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Device-Id, X-Pass');
  if (req.method === 'OPTIONS') {
    return res.sendStatus(204);
  }
  next();
});

// Authentication middleware (Web UI password check or Bearer JWT token)
const authGuard = (req, res, next) => {
  const pass = req.get('x-pass') || req.query.p || '';
  const auth = req.get('authorization') || '';

  // If APP_PASSWORD configured, check it for web requests
  if (APP_PASSWORD && pass === APP_PASSWORD) {
    return next();
  }

  // If bearer token present from Android app, accept request
  if (auth.startsWith('Bearer ') || !APP_PASSWORD) {
    return next();
  }

  res.status(401).json({ success: false, error: 'Unauthorized. Please supply authentication credentials.' });
};

// 3. API Endpoints

// Health Check
app.get(['/', '/health', '/api/health'], (req, res) => {
  res.json({
    success: true,
    service: 'FinPulse Railway WhatsApp Statement Worker',
    status: 'ONLINE',
    whatsappConnected: whatsappClient.status === 'connected',
    serverTime: new Date().toISOString()
  });
});

// SSE Stream for Real-time Dashboard
app.get('/api/events', authGuard, (req, res) => {
  res.set({
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive'
  });
  res.flushHeaders();
  res.write(`data: ${JSON.stringify(getCombinedState())}\n\n`);
  clients.add(res);
  req.on('close', () => clients.delete(res));
});

// WhatsApp Linking Status
app.get('/api/whatsapp/status', (req, res) => {
  res.json({
    success: true,
    data: whatsappClient.getStatus()
  });
});

// Request WhatsApp Pairing Code
app.post('/api/whatsapp/pair', authGuard, async (req, res) => {
  try {
    const phone = req.body.phone;
    if (!phone) {
      return res.status(400).json({ success: false, error: 'Phone number is required (with country code e.g. 919876543210)' });
    }
    const code = await whatsappClient.requestPairingCode(phone);
    res.json({ success: true, pairingCode: code });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Disconnect WhatsApp
app.post('/api/whatsapp/disconnect', authGuard, async (req, res) => {
  try {
    await whatsappClient.logout();
    res.json({ success: true, message: 'WhatsApp session disconnected and storage cleared.' });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Automation Status
app.get('/api/automation/status', (req, res) => {
  res.json({
    success: true,
    data: scheduler.getState()
  });
});

// Start Automation
app.post('/api/automation/start', authGuard, (req, res) => {
  const target = req.body.target || SBI_NUMBER;
  const intervalMinutes = req.body.intervalMinutes || 30;

  scheduler.config.targetNumber = target;
  scheduler.updateInterval(intervalMinutes);
  scheduler.start();

  res.json({
    success: true,
    message: `Automation started for target +${target} every ${intervalMinutes} minutes.`
  });
});

// Stop / Pause Automation
app.post('/api/automation/stop', authGuard, (req, res) => {
  scheduler.stop();
  res.json({
    success: true,
    message: 'Automation paused.'
  });
});

// Trigger Immediate Mini Statement Fetch
app.post('/api/automation/run-now', authGuard, async (req, res) => {
  try {
    await scheduler.executeCycle(true);
    res.json({ success: true, message: 'Mini statement cycle initiated immediately.' });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Automation Logs
app.get('/api/automation/logs', authGuard, (req, res) => {
  res.json({
    success: true,
    history: scheduler.executionHistory,
    logs: logs.slice(-50)
  });
});

// 4. Start Worker
app.listen(PORT, () => {
  log(`Server running on port ${PORT}`);
  whatsappClient.start().then(() => {
    // If scheduler was previously enabled, resume schedule
    if (scheduler.config.isEnabled) {
      log('Resuming scheduled automation cycle...');
      scheduler.scheduleNextRun();
    }
  });
});

// Handle graceful termination
process.on('SIGINT', () => process.exit(0));
process.on('SIGTERM', () => process.exit(0));
