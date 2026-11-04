const jwt = require('jsonwebtoken');
const { OAuth2Client } = require('google-auth-library');

const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);
const JWT_SECRET = process.env.JWT_SECRET || 'finpulse_jwt_secret_dev_mode_change_in_production';

// Verifies JWT token on protected endpoints
const requireAuth = (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ success: false, message: 'Authentication required. No token provided.' });
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.user = decoded; // { userId, email, name }
    next();
  } catch (err) {
    return res.status(401).json({ success: false, message: 'Invalid or expired session token.' });
  }
};

// Verifies Google OAuth token during login
const verifyGoogleToken = async (idToken) => {
  try {
    if (process.env.GOOGLE_CLIENT_ID) {
      const ticket = await googleClient.verifyIdToken({
        idToken,
        audience: process.env.GOOGLE_CLIENT_ID
      });
      const payload = ticket.getPayload();
      return {
        googleId: payload.sub,
        email: payload.email,
        name: payload.name || payload.email.split('@')[0],
        picture: payload.picture || null
      };
    } else {
      // In development or when GOOGLE_CLIENT_ID is not configured yet, decode payload safely
      const decoded = jwt.decode(idToken);
      if (decoded && decoded.sub) {
        return {
          googleId: decoded.sub,
          email: decoded.email || 'user@example.com',
          name: decoded.name || 'FinPulse User',
          picture: decoded.picture || null
        };
      }
      throw new Error('Could not parse Google ID token');
    }
  } catch (err) {
    throw new Error(`Google token validation failed: ${err.message}`);
  }
};

const generateToken = (user) => {
  return jwt.sign(
    { userId: user.id, email: user.email, name: user.name },
    JWT_SECRET,
    { expiresIn: '30d' }
  );
};

module.exports = {
  requireAuth,
  verifyGoogleToken,
  generateToken
};
