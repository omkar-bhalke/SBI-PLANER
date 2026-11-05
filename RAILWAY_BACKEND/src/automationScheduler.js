/**
 * Automation Scheduler & Execution Orchestrator
 * Controls recurring statement fetches, volume persistence, and cPanel synchronization.
 */

import fs from 'fs';
import path from 'path';
import { StatementParser } from './statementParser.js';
import { BalanceManager } from './balanceManager.js';

export class AutomationScheduler {
  constructor(options = {}) {
    this.authDir = options.authDir || process.env.AUTH_DIR || './auth';
    this.jobFilePath = path.join(this.authDir, 'job.json');
    this.whatsappClient = options.whatsappClient;
    this.cpanelClient = options.cpanelClient;
    this.onLog = options.onLog || console.log;
    this.onStateChange = options.onStateChange || (() => {});

    this.timer = null;
    this.knownHashes = new Set();
    this.lastBalance = null;
    this.lastSuccessfulFetchAt = null;
    this.executionHistory = [];

    // Default configuration
    this.config = {
      isEnabled: true,
      intervalMinutes: 30,
      targetNumber: process.env.SBI_NUMBER || '919022690226',
      defaultUserId: process.env.DEFAULT_USER_ID || 'usr_default',
      runsCount: 0,
      lastRunAt: null,
      nextRunAt: null,
      status: 'IDLE' // IDLE, RUNNING, PAUSED_DISCONNECTED, ERROR
    };

    this.loadPersistedJob();
  }

  log(msg) {
    this.onLog(`[Scheduler] ${msg}`);
  }

  loadPersistedJob() {
    try {
      if (fs.existsSync(this.jobFilePath)) {
        const data = JSON.parse(fs.readFileSync(this.jobFilePath, 'utf8'));
        this.config = { ...this.config, ...data };
        if (Array.isArray(data.knownHashes)) {
          this.knownHashes = new Set(data.knownHashes);
        }
        if (data.lastBalance !== undefined) {
          this.lastBalance = data.lastBalance;
        }
        this.log(`Loaded persisted automation job: target ${this.config.targetNumber}, every ${this.config.intervalMinutes}m`);
      }
    } catch (e) {
      this.log(`Could not read persisted job: ${e.message}`);
    }
  }

  savePersistedJob() {
    try {
      if (!fs.existsSync(this.authDir)) {
        fs.mkdirSync(this.authDir, { recursive: true });
      }
      const data = {
        ...this.config,
        knownHashes: Array.from(this.knownHashes).slice(-500),
        lastBalance: this.lastBalance,
        lastSuccessfulFetchAt: this.lastSuccessfulFetchAt
      };
      fs.writeFileSync(this.jobFilePath, JSON.stringify(data, null, 2), 'utf8');
    } catch (e) {
      this.log(`Failed to persist job state: ${e.message}`);
    }
  }

  start() {
    if (this.timer) {
      clearTimeout(this.timer);
    }
    this.config.isEnabled = true;
    this.scheduleNextRun();
    this.savePersistedJob();
    this.onStateChange();
  }

  stop() {
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }
    this.config.isEnabled = false;
    this.config.nextRunAt = null;
    this.config.status = 'PAUSED';
    this.log('Automation scheduler paused.');
    this.savePersistedJob();
    this.onStateChange();
  }

  updateInterval(minutes) {
    const val = parseInt(minutes, 10);
    if (val >= 1) {
      this.config.intervalMinutes = val;
      this.log(`Interval updated to ${val} minutes.`);
      if (this.config.isEnabled) {
        this.scheduleNextRun();
      }
      this.savePersistedJob();
      this.onStateChange();
    }
  }

  scheduleNextRun() {
    if (!this.config.isEnabled) return;

    if (this.timer) {
      clearTimeout(this.timer);
    }

    const intervalMs = this.config.intervalMinutes * 60 * 1000;
    const nextTimestamp = Date.now() + intervalMs;
    this.config.nextRunAt = new Date(nextTimestamp).toISOString();

    this.log(`Next statement fetch scheduled for ${new Date(nextTimestamp).toLocaleTimeString()} (in ${this.config.intervalMinutes}m)`);

    this.timer = setTimeout(() => {
      this.executeCycle();
    }, intervalMs);

    this.savePersistedJob();
    this.onStateChange();
  }

  /**
   * Executes a single statement fetch cycle.
   */
  async executeCycle(manual = false) {
    if (this.config.status === 'RUNNING') {
      this.log('Execution skipped: Another cycle is already running.');
      return;
    }

    if (!this.whatsappClient || this.whatsappClient.status !== 'connected') {
      this.log('Execution skipped: WhatsApp is not connected. Pausing until connection re-establishes.');
      this.config.status = 'PAUSED_DISCONNECTED';
      this.scheduleNextRun();
      return;
    }

    this.config.status = 'RUNNING';
    this.config.lastRunAt = new Date().toISOString();
    this.config.runsCount++;
    this.log(`Starting Statement Automation Cycle #${this.config.runsCount}...`);
    this.onStateChange();

    try {
      await this.whatsappClient.triggerMiniStatementCycle(this.config.targetNumber);
    } catch (err) {
      this.log(`Statement trigger error: ${err.message}`);
      this.config.status = 'ERROR';
      this.recordHistory('ERROR', null, 0, err.message);

      if (this.cpanelClient) {
        await this.cpanelClient.logAutomationCycle(this.config.defaultUserId, {
          status: 'ERROR',
          message: err.message
        });
      }

      this.scheduleNextRun();
    }
  }

  /**
   * Called when WhatsAppClient receives and extracts statement text.
   */
  async processIncomingStatement(rawText, targetNumber) {
    this.log('Processing incoming mini statement text...');

    // 1. Parse statement
    const parsed = StatementParser.parse(rawText);
    if (!parsed || !parsed.success) {
      this.log('Failed to parse statement format.');
      this.recordHistory('PARSE_ERROR', null, 0, 'Statement text format unparseable');
      this.config.status = 'IDLE';
      this.scheduleNextRun();
      return;
    }

    this.log(`Statement parsed: Account ${parsed.accountNumber}, Available Balance: ₹${parsed.availableBalance} ${parsed.balanceType}`);

    // 2. Evaluate balance & deduplicate transactions
    const evaluation = BalanceManager.evaluateStatement(parsed, this.lastBalance, this.knownHashes);
    this.log(`Evaluation: Balance diff ₹${evaluation.balanceDifference}, New transactions: ${evaluation.newTransactionsCount}`);

    // Update local memory
    for (const tx of parsed.transactions) {
      this.knownHashes.add(tx.idempotencyHash);
    }
    this.lastBalance = evaluation.currentBalance;
    this.lastSuccessfulFetchAt = new Date().toISOString();

    // 3. Sync newly detected transactions to cPanel MySQL as PENDING
    if (this.cpanelClient) {
      if (evaluation.newTransactions.length > 0) {
        this.log(`Pushing ${evaluation.newTransactions.length} new transactions to cPanel pending queue...`);
        const syncRes = await this.cpanelClient.syncPendingTransactions(
          this.config.defaultUserId,
          evaluation.newTransactions,
          parsed
        );
        this.log(`cPanel pending sync result: ${syncRes.success ? 'SUCCESS' : 'FAILED'}`);
      }

      // 4. Update balance history on cPanel
      this.log('Updating balance history on cPanel database...');
      await this.cpanelClient.updateBalanceHistory(this.config.defaultUserId, evaluation);

      // 5. Log cycle to cPanel
      await this.cpanelClient.logAutomationCycle(this.config.defaultUserId, {
        status: 'SUCCESS',
        balanceExtracted: evaluation.currentBalance,
        transactionsFound: evaluation.newTransactionsCount,
        message: `Fetched statement. Balance ₹${evaluation.currentBalance}. Diff: ₹${evaluation.balanceDifference}. ${evaluation.newTransactionsCount} new transactions queued.`
      });
    }

    this.recordHistory(
      'SUCCESS',
      evaluation.currentBalance,
      evaluation.newTransactionsCount,
      `Balance ₹${evaluation.currentBalance}. Diff ₹${evaluation.balanceDifference}. ${evaluation.newTransactionsCount} new transactions queued.`
    );

    this.config.status = 'IDLE';
    this.savePersistedJob();
    this.scheduleNextRun();
    this.onStateChange();
  }

  recordHistory(status, balance, txCount, message) {
    this.executionHistory.unshift({
      timestamp: new Date().toISOString(),
      run: this.config.runsCount,
      status,
      balance,
      transactionsCount: txCount,
      message
    });
    this.executionHistory = this.executionHistory.slice(0, 30);
  }

  getState() {
    return {
      config: this.config,
      lastBalance: this.lastBalance,
      lastSuccessfulFetchAt: this.lastSuccessfulFetchAt,
      knownTransactionsCount: this.knownHashes.size,
      executionHistory: this.executionHistory
    };
  }
}
