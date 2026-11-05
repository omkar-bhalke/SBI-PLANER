/**
 * Balance and Transaction Difference Manager
 * Calculates balance variance, detects new transactions, and manages deduplication.
 */

export class BalanceManager {
  /**
   * Evaluates the new statement against the previously recorded statement balance.
   * @param {object} parsedStatement - Output from StatementParser.parse()
   * @param {number|null} previousBalance - Previous available balance from database
   * @param {Set|Array} knownTransactionHashes - Set of already processed transaction hashes
   * @returns {object} Analysis result
   */
  static evaluateStatement(parsedStatement, previousBalance = null, knownTransactionHashes = new Set()) {
    const currentBalance = parsedStatement.availableBalance;
    const hasPrevious = previousBalance !== null && !isNaN(previousBalance);

    let balanceDifference = 0.0;
    if (hasPrevious && currentBalance !== null) {
      balanceDifference = parseFloat((currentBalance - previousBalance).toFixed(2));
    }

    // Filter transactions to detect genuinely NEW entries
    const newTransactions = [];
    const knownSet = knownTransactionHashes instanceof Set ? knownTransactionHashes : new Set(knownTransactionHashes);

    let sumNewDebits = 0.0;
    let sumNewCredits = 0.0;

    for (const tx of parsedStatement.transactions) {
      if (!knownSet.has(tx.idempotencyHash)) {
        newTransactions.push(tx);
        if (tx.type === 'DEBIT') {
          sumNewDebits += tx.amount;
        } else {
          sumNewCredits += tx.amount;
        }
      }
    }

    const netTransactionImpact = parseFloat((sumNewCredits - sumNewDebits).toFixed(2));

    // Balance consistency check
    let discrepancy = 0.0;
    let isDiscrepancyDetected = false;
    if (hasPrevious && balanceDifference !== 0 && netTransactionImpact !== 0) {
      discrepancy = parseFloat(Math.abs(balanceDifference - netTransactionImpact).toFixed(2));
      if (discrepancy > 0.01) {
        isDiscrepancyDetected = true;
      }
    }

    return {
      accountNumber: parsedStatement.accountNumber,
      currentBalance: currentBalance,
      previousBalance: hasPrevious ? previousBalance : currentBalance,
      balanceDifference: balanceDifference,
      statementDate: parsedStatement.statementDate,
      statementTime: parsedStatement.statementTime,
      totalTransactionsInStatement: parsedStatement.transactionsCount,
      newTransactionsCount: newTransactions.length,
      newTransactions: newTransactions,
      netTransactionImpact: netTransactionImpact,
      isDiscrepancyDetected: isDiscrepancyDetected,
      discrepancyAmount: discrepancy
    };
  }
}
