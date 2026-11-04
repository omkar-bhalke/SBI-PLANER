const db = require('../config/db');

exports.getSummary = async (req, res) => {
  try {
    const userId = req.user.userId;
    const today = new Date().toISOString().slice(0, 10);
    const firstDayOfMonth = today.slice(0, 8) + '01';

    // 1. Account balances
    const [accounts] = await db.query(
      'SELECT id, bank_name, opening_balance, current_balance FROM accounts WHERE user_id = ? AND is_primary = true LIMIT 1',
      [userId]
    );

    const account = accounts[0] || { opening_balance: 0, current_balance: 0, bank_name: 'SBI' };
    const initialBalance = parseFloat(account.opening_balance || 0);

    // 2. All-time Totals
    const [allTimeTotals] = await db.query(
      `SELECT 
        COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END), 0) AS total_income,
        COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS total_expense
       FROM transactions WHERE user_id = ?`,
      [userId]
    );

    const totalIncome = parseFloat(allTimeTotals[0].total_income);
    const totalExpenses = parseFloat(allTimeTotals[0].total_expense);
    const currentBalance = initialBalance + totalIncome - totalExpenses;

    // 3. User settings (Daily Spending Limit)
    const [settings] = await db.query('SELECT daily_spending_limit, currency, currency_symbol FROM user_settings WHERE user_id = ? LIMIT 1', [userId]);
    const dailyLimit = parseFloat(settings[0]?.daily_spending_limit || 500.00);

    // 4. Today's Overview
    const [todayTotals] = await db.query(
      `SELECT 
        COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END), 0) AS today_income,
        COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS today_expense
       FROM transactions WHERE user_id = ? AND transaction_date = ?`,
      [userId, today]
    );

    const todayIncome = parseFloat(todayTotals[0].today_income);
    const todayExpense = parseFloat(todayTotals[0].today_expense);
    const todayRemainingAllowance = Math.max(0, dailyLimit - todayExpense);
    const limitUtilizationPercent = dailyLimit > 0 ? (todayExpense / dailyLimit) * 100 : 0;
    const isLimitExceeded = todayExpense > dailyLimit;

    // 5. Monthly Overview
    const [monthlyTotals] = await db.query(
      `SELECT 
        COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END), 0) AS month_income,
        COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS month_expense
       FROM transactions WHERE user_id = ? AND transaction_date >= ?`,
      [userId, firstDayOfMonth]
    );

    const monthlyIncome = parseFloat(monthlyTotals[0].month_income);
    const monthlyExpenses = parseFloat(monthlyTotals[0].month_expense);
    const monthlySavings = monthlyIncome - monthlyExpenses;
    const monthlySpendingPercentage = monthlyIncome > 0 ? (monthlyExpenses / monthlyIncome) * 100 : 0;

    // 6. Recent Transactions
    const [recentTransactions] = await db.query(
      'SELECT * FROM transactions WHERE user_id = ? ORDER BY transaction_date DESC, transaction_time DESC LIMIT 10',
      [userId]
    );

    return res.json({
      success: true,
      data: {
        balance: {
          currentBalance,
          initialBalance,
          totalIncome,
          totalExpenses,
          bankName: account.bank_name
        },
        today: {
          income: todayIncome,
          expense: todayExpense,
          dailyLimit,
          remainingAllowance: todayRemainingAllowance,
          utilizationPercentage: parseFloat(limitUtilizationPercent.toFixed(1)),
          isExceeded: isLimitExceeded
        },
        monthly: {
          income: monthlyIncome,
          expense: monthlyExpenses,
          savings: monthlySavings,
          spendingPercentage: parseFloat(monthlySpendingPercentage.toFixed(1))
        },
        recentTransactions
      }
    });
  } catch (error) {
    console.error('Error computing dashboard summary:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

exports.getAnalytics = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { period = 'MONTHLY' } = req.query; // DAILY, WEEKLY, MONTHLY, YEARLY

    // 1. Category breakdown
    const [categoryExpenses] = await db.query(
      `SELECT category_name, SUM(amount) as total_amount, COUNT(*) as count
       FROM transactions 
       WHERE user_id = ? AND transaction_type = 'EXPENSE'
       GROUP BY category_name
       ORDER BY total_amount DESC`,
      [userId]
    );

    // 2. Repeated purchases detection
    const [repeatedPurchases] = await db.query(
      `SELECT description, COUNT(*) as count, SUM(amount) as total_spent, AVG(amount) as average_price
       FROM transactions 
       WHERE user_id = ? AND transaction_type = 'EXPENSE'
       GROUP BY LOWER(TRIM(description))
       HAVING count >= 2
       ORDER BY count DESC, total_spent DESC
       LIMIT 10`,
      [userId]
    );

    // 3. Daily spending aggregation for the last 14 days
    const [dailySpending] = await db.query(
      `SELECT transaction_date, SUM(amount) as total_spent
       FROM transactions 
       WHERE user_id = ? AND transaction_type = 'EXPENSE'
       GROUP BY transaction_date
       ORDER BY transaction_date DESC
       LIMIT 14`,
      [userId]
    );

    // 4. Spending stats
    const [stats] = await db.query(
      `SELECT 
        MAX(amount) as max_single_expense,
        AVG(amount) as avg_expense
       FROM transactions WHERE user_id = ? AND transaction_type = 'EXPENSE'`,
      [userId]
    );

    return res.json({
      success: true,
      data: {
        categoryBreakdown: categoryExpenses,
        repeatedPurchases,
        dailySpending: dailySpending.reverse(),
        stats: stats[0] || {}
      }
    });
  } catch (error) {
    console.error('Error fetching analytics:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};
