/**
 * SBI WhatsApp Mini Statement Parser
 * Parses official SBI WhatsApp mini statement text into structured data.
 */

import crypto from 'crypto';

export class StatementParser {
  /**
   * Parses the raw SBI mini statement text.
   * @param {string} text - Raw message string received from SBI WhatsApp
   * @returns {object|null} Parsed statement object or null if not a valid statement
   */
  static parse(text) {
    if (!text || typeof text !== 'string') {
      return null;
    }

    const cleanText = text.trim();

    // Verify it is indeed an SBI mini statement
    const isStatement = /Available\s+Balance/i.test(cleanText) ||
                        /A\/c\s+[x\*\.\d]+/i.test(cleanText) ||
                        /\d{2}[\/\-]\d{2}[\/\-]\d{2,4}:\s*\d+(\.\d+)?\s*(CR|DR)/i.test(cleanText);

    if (!isStatement) {
      return null;
    }

    // 1. Extract Statement Date & Time
    // e.g. "05/11/2026, 10:21" or "05/11/2026 10:21:00"
    let statementDate = null;
    let statementTime = null;
    let statementTimestamp = Date.now();

    const dateTimeMatch = cleanText.match(/(\d{2}[\/\-]\d{2}[\/\-]\d{2,4})[,\s]+(\d{1,2}:\d{2}(?::\d{2})?(?:\s*[AaPp][Mm])?)/);
    if (dateTimeMatch) {
      statementDate = dateTimeMatch[1];
      statementTime = dateTimeMatch[2];
    } else {
      const now = new Date();
      statementDate = now.toLocaleDateString('en-GB'); // DD/MM/YYYY
      statementTime = now.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
    }

    // 2. Extract Masked Bank Account Number
    // e.g. "Available Balance in A/c xxx188: Rs. 433.77 CR"
    let accountNumber = 'xxx188';
    const acctMatch = cleanText.match(/A\/c\s*([x\*\.\d]+)/i) || cleanText.match(/Account\s*([x\*\.\d]+)/i);
    if (acctMatch) {
      accountNumber = acctMatch[1].trim();
    }

    // 3. Extract Available / Current Balance
    // e.g. "Rs. 433.77 CR" or "Rs 433.77" or "INR 433.77 CR"
    let availableBalance = null;
    let balanceType = 'CR';

    const balMatch = cleanText.match(/Available\s+Balance[^\d]*?(?:Rs\.?|INR)?\s*([\d,]+\.?\d*)\s*(CR|DR)?/i);
    if (balMatch) {
      availableBalance = parseFloat(balMatch[1].replace(/,/g, ''));
      if (balMatch[2]) {
        balanceType = balMatch[2].toUpperCase();
      }
    }

    // 4. Extract Line-by-Line Transactions
    // Matches patterns like:
    // "04/11/2026: 20.00 DR,BY TRFR"
    // "04/11/2026: 41.00 DR,BY TRFR"
    // "03/11/2026: 100.00 CR,BY SALARY"
    // "04-11-2026: 50.00 DR UPI/123456"
    const transactions = [];
    const lines = cleanText.split(/\r?\n/);

    const txLineRegex = /(\d{2}[\/\-]\d{2}[\/\-]\d{2,4})[\s:\-]+([\d,]+\.?\d*)\s*(DR|CR)[,\s]*(.*)/i;

    for (const rawLine of lines) {
      const line = rawLine.trim();
      const match = line.match(txLineRegex);
      if (match) {
        const rawDate = match[1];
        const rawAmount = parseFloat(match[2].replace(/,/g, ''));
        const indicator = match[3].toUpperCase(); // DR or CR
        const description = (match[4] || 'BY TRANSFER').trim().replace(/^[,\-\s]+/, '') || 'BY TRFR';

        const type = indicator === 'CR' ? 'CREDIT' : 'DEBIT';

        // Normalize date to YYYY-MM-DD
        const formattedDate = this.normalizeDate(rawDate);

        // Generate deterministic idempotency hash: sha256(accountNumber + date + amount + type + description)
        const hashInput = `${accountNumber}_${formattedDate}_${rawAmount.toFixed(2)}_${type}_${description}`;
        const idempotencyHash = crypto.createHash('sha256').update(hashInput).digest('hex').substring(0, 32);

        transactions.push({
          rawLine: line,
          date: formattedDate,
          displayDate: rawDate,
          amount: rawAmount,
          type: type,
          indicator: indicator,
          rawDescription: description,
          suggestedCategory: this.suggestCategory(description, type),
          idempotencyHash: idempotencyHash
        });
      }
    }

    return {
      success: true,
      rawText: cleanText,
      statementDate: statementDate,
      statementTime: statementTime,
      statementTimestamp: statementTimestamp,
      accountNumber: accountNumber,
      availableBalance: availableBalance,
      balanceType: balanceType,
      transactionsCount: transactions.length,
      transactions: transactions
    };
  }

  /**
   * Normalizes DD/MM/YYYY or DD-MM-YYYY to YYYY-MM-DD.
   */
  static normalizeDate(dateStr) {
    if (!dateStr) return new Date().toISOString().split('T')[0];
    const parts = dateStr.split(/[\/\-]/);
    if (parts.length === 3) {
      let day = parts[0].padStart(2, '0');
      let month = parts[1].padStart(2, '0');
      let year = parts[2];
      if (year.length === 2) {
        year = '20' + year;
      }
      return `${year}-${month}-${day}`;
    }
    return dateStr;
  }

  /**
   * Suggests an initial category based on transaction description heuristics.
   * Note: As required, this is only a suggestion - the user confirms or updates it in the Android app.
   */
  static suggestCategory(description, type) {
    if (type === 'CREDIT') {
      if (/sal(?:ary)?/i.test(description)) return 'Salary & Allowance';
      if (/interest|int/i.test(description)) return 'Investments & Gifts';
      if (/refund/i.test(description)) return 'Refunds';
      return 'Income';
    }

    // Debits
    if (/swiggy|zomato|cafe|food|hotel|restaurant|chai|tea|bakery/i.test(description)) return 'Food & Dining';
    if (/dmart|blinkit|zepto|grocery|supermarket/i.test(description)) return 'Groceries';
    if (/uber|ola|rapido|metro|petrol|fuel|bus|irctc|railway/i.test(description)) return 'Transportation';
    if (/electricity|bill|bescom|jio|airtel|recharge|water|gas/i.test(description)) return 'Utilities & Bills';
    if (/hospital|pharmacy|med|apollo|clinic|dr\./i.test(description)) return 'Healthcare';
    if (/amazon|flipkart|myntra|shopping/i.test(description)) return 'Shopping';
    if (/book|fees|school|college|exam/i.test(description)) return 'Education & Books';

    return 'General Transfer';
  }
}
