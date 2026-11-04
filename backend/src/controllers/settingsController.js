const db = require('../config/db');

exports.getSettings = async (req, res) => {
  try {
    const userId = req.user.userId;
    const [settings] = await db.query('SELECT * FROM user_settings WHERE user_id = ? LIMIT 1', [userId]);
    const [accounts] = await db.query('SELECT * FROM accounts WHERE user_id = ? AND is_primary = true LIMIT 1', [userId]);

    return res.json({
      success: true,
      settings: settings[0] || {},
      account: accounts[0] || {}
    });
  } catch (error) {
    console.error('Error fetching settings:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

exports.updateSettings = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { dailySpendingLimit, currency, theme, onboardingCompleted, selectedBank, initialBalance, aiProvider } = req.body;

    // Update user_settings
    await db.query(
      `UPDATE user_settings SET 
        daily_spending_limit = COALESCE(?, daily_spending_limit),
        currency = COALESCE(?, currency),
        theme = COALESCE(?, theme),
        onboarding_completed = COALESCE(?, onboarding_completed),
        selected_bank = COALESCE(?, selected_bank),
        ai_provider = COALESCE(?, ai_provider)
       WHERE user_id = ?`,
      [dailySpendingLimit, currency, theme, onboardingCompleted, selectedBank, aiProvider, userId]
    );

    // Update primary account if initialBalance or bank specified
    if (initialBalance !== undefined || selectedBank !== undefined) {
      const [acc] = await db.query('SELECT id, opening_balance FROM accounts WHERE user_id = ? AND is_primary = true LIMIT 1', [userId]);
      if (acc.length > 0) {
        const openingBal = initialBalance !== undefined ? parseFloat(initialBalance) : parseFloat(acc[0].opening_balance);
        await db.query(
          'UPDATE accounts SET opening_balance = ?, bank_name = COALESCE(?, bank_name) WHERE id = ?',
          [openingBal, selectedBank || null, acc[0].id]
        );

        // Recalculate balance
        const [totals] = await db.query(
          `SELECT 
            COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END), 0) AS total_income,
            COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS total_expense
           FROM transactions WHERE user_id = ? AND (account_id = ? OR account_id IS NULL)`,
          [userId, acc[0].id]
        );
        const newCurrent = openingBal + parseFloat(totals[0].total_income) - parseFloat(totals[0].total_expense);
        await db.query('UPDATE accounts SET current_balance = ? WHERE id = ?', [newCurrent, acc[0].id]);
      }
    }

    const [updatedSettings] = await db.query('SELECT * FROM user_settings WHERE user_id = ? LIMIT 1', [userId]);
    const [updatedAccount] = await db.query('SELECT * FROM accounts WHERE user_id = ? AND is_primary = true LIMIT 1', [userId]);

    return res.json({
      success: true,
      settings: updatedSettings[0] || {},
      account: updatedAccount[0] || {}
    });
  } catch (error) {
    console.error('Error updating settings:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};
