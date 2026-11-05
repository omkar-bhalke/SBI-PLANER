/**
 * Authenticated cPanel Client
 * Communicates with the cPanel MySQL REST API at https://radhewins.buzz
 */

export class CpanelClient {
  constructor(baseUrl, apiSecret, defaultUserId = null) {
    this.baseUrl = (baseUrl || 'https://radhewins.buzz').replace(/\/+$/, '');
    this.apiSecret = apiSecret || process.env.CPANEL_API_SECRET || 'finpulse_internal_railway_secret_2026';
    this.defaultUserId = defaultUserId || process.env.DEFAULT_USER_ID || null;
  }

  getHeaders(userId = null) {
    const headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'X-Internal-Secret': this.apiSecret
    };
    if (userId || this.defaultUserId) {
      headers['X-User-Id'] = userId || this.defaultUserId;
    }
    return headers;
  }

  /**
   * Pushes newly detected statement transactions to cPanel MySQL pending queue.
   */
  async syncPendingTransactions(userId, transactions, statementInfo) {
    try {
      const url = `${this.baseUrl}/api/pending-transactions/sync`;
      const payload = {
        userId: userId || this.defaultUserId,
        accountMasked: statementInfo.accountNumber,
        statementTime: `${statementInfo.statementDate} ${statementInfo.statementTime}`,
        transactions: transactions.map(t => ({
          amount: t.amount,
          type: t.type,
          rawDescription: t.rawDescription,
          suggestedCategory: t.suggestedCategory,
          transactionDate: t.date,
          idempotencyHash: t.idempotencyHash
        }))
      };

      const response = await fetch(url, {
        method: 'POST',
        headers: this.getHeaders(userId),
        body: JSON.stringify(payload)
      });

      const data = await response.json();
      return { success: response.ok && data.success, data };
    } catch (err) {
      console.error('[cPanel Client] syncPendingTransactions error:', err.message);
      return { success: false, error: err.message };
    }
  }

  /**
   * Updates balance and logs statement history in cPanel MySQL.
   */
  async updateBalanceHistory(userId, balanceData) {
    try {
      const url = `${this.baseUrl}/api/balance/update`;
      const payload = {
        userId: userId || this.defaultUserId,
        accountMasked: balanceData.accountNumber,
        currentBalance: balanceData.currentBalance,
        previousBalance: balanceData.previousBalance,
        balanceDifference: balanceData.balanceDifference,
        statementTimestamp: `${balanceData.statementDate} ${balanceData.statementTime}`,
        newTransactionsCount: balanceData.newTransactionsCount
      };

      const response = await fetch(url, {
        method: 'POST',
        headers: this.getHeaders(userId),
        body: JSON.stringify(payload)
      });

      const data = await response.json();
      return { success: response.ok && data.success, data };
    } catch (err) {
      console.error('[cPanel Client] updateBalanceHistory error:', err.message);
      return { success: false, error: err.message };
    }
  }

  /**
   * Logs an automation execution cycle to cPanel MySQL.
   */
  async logAutomationCycle(userId, logData) {
    try {
      const url = `${this.baseUrl}/api/automation/log`;
      const payload = {
        userId: userId || this.defaultUserId,
        status: logData.status,
        balanceExtracted: logData.balanceExtracted,
        transactionsFound: logData.transactionsFound || 0,
        message: logData.message
      };

      const response = await fetch(url, {
        method: 'POST',
        headers: this.getHeaders(userId),
        body: JSON.stringify(payload)
      });

      return response.ok;
    } catch (err) {
      console.error('[cPanel Client] logAutomationCycle error:', err.message);
      return false;
    }
  }

  /**
   * Fetches the user's latest automation settings and last known balance.
   */
  async getAutomationConfig(userId) {
    try {
      const url = `${this.baseUrl}/api/automation/config`;
      const response = await fetch(url, {
        method: 'GET',
        headers: this.getHeaders(userId)
      });
      if (response.ok) {
        const data = await response.json();
        return data.config || null;
      }
    } catch (err) {
      console.error('[cPanel Client] getAutomationConfig error:', err.message);
    }
    return null;
  }
}
