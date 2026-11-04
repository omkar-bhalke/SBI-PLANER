const db = require('../config/db');

exports.getIntegrations = async (req, res) => {
  try {
    const userId = req.user.userId;
    const [integrations] = await db.query('SELECT * FROM integrations WHERE user_id = ?', [userId]);

    return res.json({
      success: true,
      integrations
    });
  } catch (error) {
    console.error('Error fetching integrations:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

exports.connectWhatsApp = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { phoneNumber } = req.body;

    if (!phoneNumber) {
      return res.status(400).json({ success: false, message: 'Valid phone number is required' });
    }

    // In Version 1, prepare the integration structure without fake simulation
    await db.query(
      `INSERT INTO integrations (id, user_id, integration_type, connection_status, phone_number, configuration_reference)
       VALUES (?, ?, 'WHATSAPP', 'PENDING', ?, ?)
       ON DUPLICATE KEY UPDATE connection_status = 'PENDING', phone_number = VALUES(phone_number), updated_at = CURRENT_TIMESTAMP`,
      [require('uuid').v4(), userId, phoneNumber, JSON.stringify({ verified: false, pairingCodeRequested: true })]
    );

    return res.json({
      success: true,
      status: 'PENDING',
      message: 'WhatsApp pairing request initiated. The pairing gateway is ready for future webhook activation.'
    });
  } catch (error) {
    console.error('Error connecting WhatsApp:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

exports.disconnectWhatsApp = async (req, res) => {
  try {
    const userId = req.user.userId;
    await db.query(
      "UPDATE integrations SET connection_status = 'DISCONNECTED', phone_number = NULL WHERE user_id = ? AND integration_type = 'WHATSAPP'",
      [userId]
    );

    return res.json({
      success: true,
      status: 'DISCONNECTED',
      message: 'WhatsApp disconnected successfully.'
    });
  } catch (error) {
    console.error('Error disconnecting WhatsApp:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};
