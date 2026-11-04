const jwt = require('jsonwebtoken');
const db = require('../config/db');

const JWT_SECRET = process.env.JWT_SECRET || 'finpulse_jwt_secret_dev_mode_change_in_production';
const JWT_REFRESH_SECRET = process.env.JWT_REFRESH_SECRET || 'finpulse_jwt_refresh_secret_key_change_in_production';

// Verifies JWT access token on protected endpoints
const requireAuth = async (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ success: false, message: 'Authentication required. No token provided.' });
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.user = decoded; // { userId, email, name }

    // Task 2: Verify device binding on every authenticated session
    const clientDeviceId = req.headers['x-device-id'];
    if (clientDeviceId) {
      const [bindings] = await db.query(
        'SELECT * FROM device_bindings WHERE user_id = ? AND status = "ACTIVE" LIMIT 1',
        [decoded.userId]
      );

      if (bindings.length > 0 && bindings[0].device_id !== clientDeviceId) {
        return res.status(403).json({
          success: false,
          code: 'DEVICE_MISMATCH',
          message: 'Security Alert: Active account is bound to another physical device. Session access denied.'
        });
      }
    }

    next();
  } catch (err) {
    return res.status(401).json({ success: false, message: 'Invalid or expired session token.' });
  }
};

const generateTokens = (user) => {
  const accessToken = jwt.sign(
    { userId: user.id, email: user.email, name: user.name },
    JWT_SECRET,
    { expiresIn: '7d' }
  );

  const refreshToken = jwt.sign(
    { userId: user.id, email: user.email },
    JWT_REFRESH_SECRET,
    { expiresIn: '90d' }
  );

  return { accessToken, refreshToken };
};

const verifyRefreshToken = (token) => {
  try {
    return jwt.verify(token, JWT_REFRESH_SECRET);
  } catch (err) {
    return null;
  }
};

module.exports = {
  requireAuth,
  generateTokens,
  verifyRefreshToken
};
