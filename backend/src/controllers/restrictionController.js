const { v4: uuidv4 } = require('uuid');
const db = require('../config/db');

// Default popular Indian payment apps to restrict when spending limit exceeded
const DEFAULT_PAYMENT_PACKAGES = [
  { packageName: 'com.google.android.apps.nbu.paisa.user', appName: 'Google Pay' },
  { packageName: 'com.phonepe.app', appName: 'PhonePe' },
  { packageName: 'net.one97.paytm', appName: 'Paytm' },
  { packageName: 'in.org.npci.upiapp', appName: 'BHIM UPI' },
  { packageName: 'com.dreamplug.androidapp', appName: 'CRED' },
  { packageName: 'in.amazon.mShop.android.shopping', appName: 'Amazon Pay' },
  { packageName: 'com.whatsapp', appName: 'WhatsApp Payments' },
  { packageName: 'com.sbi.lotusintouch', appName: 'YONO SBI' }
];

exports.getRestrictions = async (req, res) => {
  try {
    const userId = req.user.userId;
    const [saved] = await db.query(
      'SELECT package_name, app_name, is_enabled FROM app_restrictions WHERE user_id = ?',
      [userId]
    );

    if (saved.length === 0) {
      // Return defaults
      return res.json({
        success: true,
        restrictions: DEFAULT_PAYMENT_PACKAGES.map(p => ({ ...p, isEnabled: true }))
      });
    }

    return res.json({
      success: true,
      restrictions: saved.map(s => ({
        packageName: s.package_name,
        appName: s.app_name,
        isEnabled: Boolean(s.is_enabled)
      }))
    });
  } catch (error) {
    console.error('Get restrictions error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

exports.updateRestrictions = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { restrictions = [] } = req.body;

    for (const item of restrictions) {
      if (!item.packageName || !item.appName) continue;
      await db.query(
        `INSERT INTO app_restrictions (id, user_id, package_name, app_name, is_enabled)
         VALUES (?, ?, ?, ?, ?)
         ON DUPLICATE KEY UPDATE is_enabled = VALUES(is_enabled), app_name = VALUES(app_name)`,
        [uuidv4(), userId, item.packageName, item.appName, Boolean(item.isEnabled)]
      );
    }

    return res.json({
      success: true,
      message: 'App restrictions updated successfully.'
    });
  } catch (error) {
    console.error('Update restrictions error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

exports.logRestrictionEvent = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { packageName, appName, dailyLimit, amountSpent, amountExceeded, actionTaken = 'BLOCKED' } = req.body;

    await db.query(
      `INSERT INTO app_restriction_events (id, user_id, package_name, app_name, daily_limit, amount_spent, amount_exceeded, action_taken)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        uuidv4(),
        userId,
        packageName || 'unknown',
        appName || 'Payment Application',
        parseFloat(dailyLimit || 0),
        parseFloat(amountSpent || 0),
        parseFloat(amountExceeded || 0),
        actionTaken
      ]
    );

    return res.json({ success: true, message: 'Restriction event recorded.' });
  } catch (error) {
    console.error('Log restriction event error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};
