const { v4: uuidv4 } = require('uuid');
const db = require('../config/db');
const { verifyGoogleToken, generateToken } = require('../middleware/auth');

exports.googleLogin = async (req, res) => {
  try {
    const { idToken, email: clientEmail, name: clientName } = req.body;
    let googleUser;

    if (idToken) {
      googleUser = await verifyGoogleToken(idToken);
    } else if (clientEmail) {
      // Direct development / testing fallback
      googleUser = {
        googleId: `google_${Date.now()}`,
        email: clientEmail,
        name: clientName || clientEmail.split('@')[0],
        picture: null
      };
    } else {
      return res.status(400).json({ success: false, message: 'Google idToken or email is required' });
    }

    // Check if user exists
    const [existingUsers] = await db.query(
      'SELECT * FROM users WHERE email = ? OR google_id = ? LIMIT 1',
      [googleUser.email, googleUser.googleId]
    );

    let user;
    let isFirstTime = false;

    if (existingUsers.length > 0) {
      user = existingUsers[0];
      // Update profile picture and name if changed
      await db.query(
        'UPDATE users SET name = ?, profile_image = COALESCE(?, profile_image) WHERE id = ?',
        [googleUser.name, googleUser.picture, user.id]
      );
    } else {
      isFirstTime = true;
      const newUserId = uuidv4();
      await db.query(
        'INSERT INTO users (id, google_id, email, name, profile_image) VALUES (?, ?, ?, ?, ?)',
        [newUserId, googleUser.googleId, googleUser.email, googleUser.name, googleUser.picture]
      );

      // Initialize default settings
      await db.query(
        'INSERT INTO user_settings (id, user_id, currency, daily_spending_limit, onboarding_completed, selected_bank) VALUES (?, ?, ?, ?, ?, ?)',
        [uuidv4(), newUserId, 'INR', 500.00, false, 'State Bank of India (SBI)']
      );

      // Initialize primary account
      await db.query(
        'INSERT INTO accounts (id, user_id, bank_name, opening_balance, current_balance, is_primary) VALUES (?, ?, ?, ?, ?, ?)',
        [uuidv4(), newUserId, 'State Bank of India (SBI)', 0.00, 0.00, true]
      );

      // Clone system default categories for the new user
      const [defaultCats] = await db.query('SELECT * FROM categories WHERE user_id IS NULL');
      for (const cat of defaultCats) {
        await db.query(
          'INSERT INTO categories (id, user_id, category_name, category_type, icon_name, color_hex, is_default) VALUES (?, ?, ?, ?, ?, ?, ?)',
          [uuidv4(), newUserId, cat.category_name, cat.category_type, cat.icon_name, cat.color_hex, true]
        );
      }

      user = {
        id: newUserId,
        email: googleUser.email,
        name: googleUser.name,
        profile_image: googleUser.picture
      };
    }

    // Fetch user settings
    const [settings] = await db.query('SELECT * FROM user_settings WHERE user_id = ? LIMIT 1', [user.id]);
    const [account] = await db.query('SELECT * FROM accounts WHERE user_id = ? AND is_primary = true LIMIT 1', [user.id]);

    const token = generateToken(user);

    return res.json({
      success: true,
      token,
      isFirstTime,
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        profileImage: user.profile_image
      },
      settings: settings[0] || null,
      account: account[0] || null
    });
  } catch (error) {
    console.error('Login error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

exports.getProfile = async (req, res) => {
  try {
    const userId = req.user.userId;
    const [users] = await db.query('SELECT id, email, name, profile_image, created_at FROM users WHERE id = ?', [userId]);
    if (users.length === 0) {
      return res.status(404).json({ success: false, message: 'User not found' });
    }

    const [settings] = await db.query('SELECT * FROM user_settings WHERE user_id = ?', [userId]);
    const [accounts] = await db.query('SELECT * FROM accounts WHERE user_id = ?', [userId]);

    return res.json({
      success: true,
      user: users[0],
      settings: settings[0] || null,
      accounts
    });
  } catch (error) {
    console.error('Profile fetch error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};
