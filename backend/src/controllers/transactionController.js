const { v4: uuidv4 } = require('uuid');
const db = require('../config/db');

// Recalculates user current account balance based on opening balance and all transactions
async function recalculateAccountBalance(userId, accountId) {
  const [accountRows] = await db.query('SELECT opening_balance FROM accounts WHERE id = ? AND user_id = ?', [accountId, userId]);
  if (accountRows.length === 0) return;

  const openingBalance = parseFloat(accountRows[0].opening_balance || 0);

  const [totals] = await db.query(
    `SELECT 
      COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END), 0) AS total_income,
      COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS total_expense
     FROM transactions WHERE user_id = ? AND (account_id = ? OR account_id IS NULL)`,
    [userId, accountId]
  );

  const currentBalance = openingBalance + parseFloat(totals[0].total_income) - parseFloat(totals[0].total_expense);
  await db.query('UPDATE accounts SET current_balance = ? WHERE id = ?', [currentBalance, accountId]);
}

exports.getTransactions = async (req, res) => {
  try {
    const userId = req.user.userId;
    const {
      type,
      category,
      startDate,
      endDate,
      search,
      sortBy = 'transaction_date',
      sortOrder = 'DESC',
      page = 1,
      limit = 50
    } = req.query;

    let query = 'SELECT * FROM transactions WHERE user_id = ?';
    const params = [userId];

    if (type && (type === 'INCOME' || type === 'EXPENSE')) {
      query += ' AND transaction_type = ?';
      params.push(type);
    }

    if (category) {
      query += ' AND category_name = ?';
      params.push(category);
    }

    if (startDate) {
      query += ' AND transaction_date >= ?';
      params.push(startDate);
    }

    if (endDate) {
      query += ' AND transaction_date <= ?';
      params.push(endDate);
    }

    if (search) {
      query += ' AND (description LIKE ? OR notes LIKE ?)';
      params.push(`%${search}%`, `%${search}%`);
    }

    const allowedSortFields = ['transaction_date', 'amount', 'created_at', 'category_name'];
    const safeSortBy = allowedSortFields.includes(sortBy) ? sortBy : 'transaction_date';
    const safeOrder = sortOrder.toUpperCase() === 'ASC' ? 'ASC' : 'DESC';

    query += ` ORDER BY ${safeSortBy} ${safeOrder}, transaction_time ${safeOrder}`;

    const offset = (parseInt(page, 10) - 1) * parseInt(limit, 10);
    query += ' LIMIT ? OFFSET ?';
    params.push(parseInt(limit, 10), offset);

    const [transactions] = await db.query(query, params);

    // Get total count for pagination
    const [countResult] = await db.query(
      'SELECT COUNT(*) as total FROM transactions WHERE user_id = ?',
      [userId]
    );

    return res.json({
      success: true,
      transactions,
      pagination: {
        page: parseInt(page, 10),
        limit: parseInt(limit, 10),
        total: countResult[0].total
      }
    });
  } catch (error) {
    console.error('Error fetching transactions:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

exports.createTransaction = async (req, res) => {
  try {
    const userId = req.user.userId;
    const {
      amount,
      type,
      description,
      categoryName,
      categoryId,
      transactionDate,
      transactionTime,
      timestamp,
      notes,
      idempotencyKey,
      accountId
    } = req.body;

    if (!amount || !type || !description || !categoryName) {
      return res.status(400).json({ success: false, message: 'Missing required transaction fields' });
    }

    // Check idempotency to prevent duplicate offline synchronization
    if (idempotencyKey) {
      const [existing] = await db.query('SELECT * FROM transactions WHERE idempotency_key = ? LIMIT 1', [idempotencyKey]);
      if (existing.length > 0) {
        return res.json({ success: true, transaction: existing[0], duplicatePrevented: true });
      }
    }

    // Find primary account if accountId not provided
    let targetAccountId = accountId;
    if (!targetAccountId) {
      const [primaryAcc] = await db.query('SELECT id FROM accounts WHERE user_id = ? AND is_primary = true LIMIT 1', [userId]);
      targetAccountId = primaryAcc.length > 0 ? primaryAcc[0].id : null;
    }

    const transactionId = uuidv4();
    const date = transactionDate || new Date().toISOString().slice(0, 10);
    const time = transactionTime || new Date().toTimeString().slice(0, 8);
    const timeMs = timestamp || Date.now();

    await db.query(
      `INSERT INTO transactions (id, user_id, account_id, transaction_type, amount, description, category_id, category_name, transaction_date, transaction_time, transaction_timestamp, notes, idempotency_key)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        transactionId,
        userId,
        targetAccountId,
        type,
        parseFloat(amount),
        description.trim(),
        categoryId || null,
        categoryName.trim(),
        date,
        time,
        timeMs,
        notes || null,
        idempotencyKey || null
      ]
    );

    if (targetAccountId) {
      await recalculateAccountBalance(userId, targetAccountId);
    }

    const [created] = await db.query('SELECT * FROM transactions WHERE id = ?', [transactionId]);

    return res.status(201).json({
      success: true,
      transaction: created[0]
    });
  } catch (error) {
    console.error('Error creating transaction:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

exports.updateTransaction = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { id } = req.params;
    const { amount, type, description, categoryName, categoryId, transactionDate, transactionTime, notes } = req.body;

    const [existing] = await db.query('SELECT * FROM transactions WHERE id = ? AND user_id = ?', [id, userId]);
    if (existing.length === 0) {
      return res.status(404).json({ success: false, message: 'Transaction not found or unauthorized' });
    }

    await db.query(
      `UPDATE transactions SET 
        amount = COALESCE(?, amount),
        transaction_type = COALESCE(?, transaction_type),
        description = COALESCE(?, description),
        category_name = COALESCE(?, category_name),
        category_id = COALESCE(?, category_id),
        transaction_date = COALESCE(?, transaction_date),
        transaction_time = COALESCE(?, transaction_time),
        notes = COALESCE(?, notes)
       WHERE id = ? AND user_id = ?`,
      [
        amount ? parseFloat(amount) : null,
        type || null,
        description ? description.trim() : null,
        categoryName ? categoryName.trim() : null,
        categoryId || null,
        transactionDate || null,
        transactionTime || null,
        notes,
        id,
        userId
      ]
    );

    if (existing[0].account_id) {
      await recalculateAccountBalance(userId, existing[0].account_id);
    }

    const [updated] = await db.query('SELECT * FROM transactions WHERE id = ?', [id]);

    return res.json({ success: true, transaction: updated[0] });
  } catch (error) {
    console.error('Error updating transaction:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

exports.deleteTransaction = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { id } = req.params;

    const [existing] = await db.query('SELECT * FROM transactions WHERE id = ? AND user_id = ?', [id, userId]);
    if (existing.length === 0) {
      return res.status(404).json({ success: false, message: 'Transaction not found or unauthorized' });
    }

    const accountId = existing[0].account_id;
    await db.query('DELETE FROM transactions WHERE id = ? AND user_id = ?', [id, userId]);

    if (accountId) {
      await recalculateAccountBalance(userId, accountId);
    }

    return res.json({ success: true, message: 'Transaction deleted successfully' });
  } catch (error) {
    console.error('Error deleting transaction:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

// Batch synchronization endpoint for offline transactions
exports.syncBatch = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { pendingTransactions = [] } = req.body;

    const syncedResults = [];

    for (const item of pendingTransactions) {
      if (!item.amount || !item.type || !item.description) continue;

      // Deduplication check via idempotencyKey
      const key = item.idempotencyKey || item.clientLocalId;
      if (key) {
        const [existing] = await db.query('SELECT * FROM transactions WHERE idempotency_key = ? LIMIT 1', [key]);
        if (existing.length > 0) {
          syncedResults.push({ clientLocalId: item.clientLocalId, serverId: existing[0].id, status: 'ALREADY_SYNCED' });
          continue;
        }
      }

      const transactionId = uuidv4();
      await db.query(
        `INSERT INTO transactions (id, user_id, transaction_type, amount, description, category_name, transaction_date, transaction_time, transaction_timestamp, notes, idempotency_key)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          transactionId,
          userId,
          item.type,
          parseFloat(item.amount),
          item.description,
          item.categoryName || 'Other Expenses',
          item.transactionDate || new Date().toISOString().slice(0, 10),
          item.transactionTime || new Date().toTimeString().slice(0, 8),
          item.timestamp || Date.now(),
          item.notes || null,
          key
        ]
      );

      syncedResults.push({ clientLocalId: item.clientLocalId, serverId: transactionId, status: 'SYNCED' });
    }

    // Recalculate primary account balance
    const [acc] = await db.query('SELECT id FROM accounts WHERE user_id = ? AND is_primary = true LIMIT 1', [userId]);
    if (acc.length > 0) {
      await recalculateAccountBalance(userId, acc[0].id);
    }

    return res.json({ success: true, results: syncedResults });
  } catch (error) {
    console.error('Batch sync error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};
