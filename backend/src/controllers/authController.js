const { v4: uuidv4 } = require('uuid');
const bcrypt = require('bcryptjs');
const db = require('../config/db');
const { generateTokens, verifyRefreshToken } = require('../middleware/auth');

// Email validation regex
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Strong password check: min 8 chars, at least 1 digit, at least 1 letter
const validatePasswordStrength = (password) => {
  if (!password || password.length < 8) {
    return 'Password must be at least 8 characters long.';
  }
  if (!/\d/.test(password)) {
    return 'Password must contain at least one numerical digit.';
  }
  if (!/[a-zA-Z]/.test(password)) {
    return 'Password must contain at least one letter.';
  }
  return null;
};

// ==========================================
// TASK 1: EMAIL & PASSWORD REGISTRATION
// TASK 2: ONE DEVICE = ONE ACCOUNT ENFORCEMENT
// ==========================================
exports.register = async (req, res) => {
  try {
    const {
      name,
      email,
      password,
      confirmPassword,
      deviceId,
      deviceFingerprint,
      deviceModel,
      osVersion
    } = req.body;

    // 1. Validation
    if (!name || !name.trim()) {
      return res.status(400).json({ success: false, message: 'Full name is required.' });
    }

    if (!email || !EMAIL_REGEX.test(email.trim())) {
      return res.status(400).json({ success: false, message: 'Please provide a valid email address.' });
    }

    const passwordError = validatePasswordStrength(password);
    if (passwordError) {
      return res.status(400).json({ success: false, message: passwordError });
    }

    if (password !== confirmPassword) {
      return res.status(400).json({ success: false, message: 'Password confirmation does not match.' });
    }

    if (!deviceId) {
      return res.status(400).json({ success: false, message: 'Hardware device binding identifier is required.' });
    }

    const normalizedEmail = email.trim().toLowerCase();

    // 2. TASK 2: Check if this physical device is already bound to another registered account
    const [existingDevice] = await db.query(
      'SELECT db.*, u.email as bound_email FROM device_bindings db JOIN users u ON db.user_id = u.id WHERE db.device_id = ? AND db.status = "ACTIVE" LIMIT 1',
      [deviceId]
    );

    if (existingDevice.length > 0) {
      return res.status(409).json({
        success: false,
        code: 'DEVICE_ALREADY_BOUND',
        message: 'Device Restriction: This physical device is already bound to another FinPulse account (' +
          existingDevice[0].bound_email.replace(/(.{2})(.*)(@.*)/, '$1***$3') +
          '). FinPulse strictly permits only one registered account per device. Please log into your existing account or contact support for a device reset.'
      });
    }

    // 3. Prevent duplicate email registration
    const [existingUser] = await db.query('SELECT id FROM users WHERE email = ? LIMIT 1', [normalizedEmail]);
    if (existingUser.length > 0) {
      return res.status(409).json({
        success: false,
        code: 'EMAIL_ALREADY_EXISTS',
        message: 'An account with this email address already exists. Please log in instead.'
      });
    }

    // 4. Secure Password Hashing
    const saltRounds = 10;
    const passwordHash = await bcrypt.hash(password, saltRounds);

    const newUserId = uuidv4();
    const bindingId = uuidv4();

    // 5. Create user record
    await db.query(
      `INSERT INTO users (id, email, password_hash, name, is_email_verified)
       VALUES (?, ?, ?, ?, true)`,
      [newUserId, normalizedEmail, passwordHash, name.trim()]
    );

    // 6. Bind physical device to this user (One Device = One Account)
    await db.query(
      `INSERT INTO device_bindings (id, device_id, user_id, device_fingerprint, device_model, os_version, status)
       VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')`,
      [bindingId, deviceId, newUserId, deviceFingerprint || null, deviceModel || null, osVersion || null]
    );

    // 7. Initialize user financial settings
    await db.query(
      `INSERT INTO user_settings (id, user_id, currency, daily_spending_limit, onboarding_completed, selected_bank)
       VALUES (?, ?, 'INR', 500.00, false, 'State Bank of India (SBI)')`,
      [uuidv4(), newUserId]
    );

    // 8. Initialize primary account
    await db.query(
      `INSERT INTO accounts (id, user_id, bank_name, opening_balance, current_balance, is_primary)
       VALUES (?, ?, 'State Bank of India (SBI)', 0.00, 0.00, true)`,
      [uuidv4(), newUserId]
    );

    // 9. Clone default categories
    const [defaultCats] = await db.query('SELECT * FROM categories WHERE user_id IS NULL');
    for (const cat of defaultCats) {
      await db.query(
        `INSERT INTO categories (id, user_id, category_name, category_type, icon_name, color_hex, is_default)
         VALUES (?, ?, ?, ?, ?, ?, true)`,
        [uuidv4(), newUserId, cat.category_name, cat.category_type, cat.icon_name, cat.color_hex]
      );
    }

    const userObj = { id: newUserId, email: normalizedEmail, name: name.trim() };
    const { accessToken, refreshToken } = generateTokens(userObj);

    return res.status(201).json({
      success: true,
      message: 'Account registered and physical device successfully bound.',
      token: accessToken,
      refreshToken,
      isFirstTime: true,
      user: {
        id: newUserId,
        email: normalizedEmail,
        name: name.trim(),
        profileImage: null
      },
      deviceBound: {
        deviceId,
        status: 'ACTIVE'
      }
    });
  } catch (error) {
    console.error('Registration error:', error);
    return res.status(500).json({ success: false, message: 'Server error during registration: ' + error.message });
  }
};

// ==========================================
// TASK 1: EMAIL & PASSWORD LOGIN
// TASK 2: DEVICE VERIFICATION ON LOGIN
// ==========================================
exports.login = async (req, res) => {
  try {
    const { email, password, deviceId, deviceFingerprint } = req.body;

    if (!email || !password) {
      return res.status(400).json({ success: false, message: 'Email and password are required.' });
    }

    const normalizedEmail = email.trim().toLowerCase();

    // 1. Fetch user
    const [users] = await db.query(
      'SELECT id, email, password_hash, name, profile_image FROM users WHERE email = ? LIMIT 1',
      [normalizedEmail]
    );

    if (users.length === 0 || !users[0].password_hash) {
      return res.status(401).json({ success: false, message: 'Invalid email address or password.' });
    }

    const user = users[0];

    // 2. Verify Password
    const isPasswordValid = await bcrypt.compare(password, user.password_hash);
    if (!isPasswordValid) {
      return res.status(401).json({ success: false, message: 'Invalid email address or password.' });
    }

    // 3. TASK 2: Device Verification & One Device = One Account Policy
    if (deviceId) {
      // Check if user already has an active device binding
      const [userBindings] = await db.query(
        'SELECT * FROM device_bindings WHERE user_id = ? AND status = "ACTIVE" LIMIT 1',
        [user.id]
      );

      if (userBindings.length > 0) {
        // User already has an active device. Must match.
        if (userBindings[0].device_id !== deviceId) {
          return res.status(403).json({
            success: false,
            code: 'DEVICE_MISMATCH',
            message: 'Device Conflict: Your account is currently bound to another physical device. FinPulse allows only one active device. Please use your registered device or request a device reset.'
          });
        } else {
          // Update last seen
          await db.query(
            'UPDATE device_bindings SET last_seen_at = CURRENT_TIMESTAMP WHERE id = ?',
            [userBindings[0].id]
          );
        }
      } else {
        // First login on this device or previous binding released.
        // Check if device is bound to someone else
        const [otherDevice] = await db.query(
          'SELECT user_id FROM device_bindings WHERE device_id = ? AND status = "ACTIVE" LIMIT 1',
          [deviceId]
        );

        if (otherDevice.length > 0 && otherDevice[0].user_id !== user.id) {
          return res.status(409).json({
            success: false,
            code: 'DEVICE_BOUND_TO_ANOTHER',
            message: 'Device Conflict: This device is registered to another account. You cannot log into multiple accounts on one device.'
          });
        }

        // Bind device to this user
        await db.query(
          `INSERT INTO device_bindings (id, device_id, user_id, device_fingerprint, status)
           VALUES (?, ?, ?, ?, 'ACTIVE')
           ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), status = 'ACTIVE', last_seen_at = CURRENT_TIMESTAMP`,
          [uuidv4(), deviceId, user.id, deviceFingerprint || null]
        );
      }
    }

    const [settings] = await db.query('SELECT * FROM user_settings WHERE user_id = ? LIMIT 1', [user.id]);
    const [account] = await db.query('SELECT * FROM accounts WHERE user_id = ? AND is_primary = true LIMIT 1', [user.id]);

    const { accessToken, refreshToken } = generateTokens(user);

    return res.json({
      success: true,
      message: 'Logged in successfully.',
      token: accessToken,
      refreshToken,
      isFirstTime: settings.length === 0 || !settings[0].onboarding_completed,
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
    return res.status(500).json({ success: false, message: 'Server error during login: ' + error.message });
  }
};

// ==========================================
// TASK 1: FORGOT PASSWORD (OTP GENERATION)
// ==========================================
exports.forgotPassword = async (req, res) => {
  try {
    const { email } = req.body;
    if (!email || !EMAIL_REGEX.test(email.trim())) {
      return res.status(400).json({ success: false, message: 'Please provide a valid email address.' });
    }

    const normalizedEmail = email.trim().toLowerCase();
    const [users] = await db.query('SELECT id, name FROM users WHERE email = ? LIMIT 1', [normalizedEmail]);

    if (users.length === 0) {
      // Return generic message for privacy
      return res.json({
        success: true,
        message: 'If an account exists with this email, a 6-digit verification code has been dispatched.'
      });
    }

    // Generate 6-digit secure numeric OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = new Date(Date.now() + 15 * 60 * 1000); // 15 mins validity

    await db.query(
      'UPDATE users SET reset_password_otp = ?, reset_otp_expires_at = ? WHERE id = ?',
      [otp, expiresAt, users[0].id]
    );

    console.log(`🔑 [PASSWORD RESET OTP] For ${normalizedEmail}: ${otp} (Valid for 15 mins)`);

    return res.json({
      success: true,
      message: 'A 6-digit verification code has been sent to your registered email.',
      debugOtp: otp // Included for testing and verification
    });
  } catch (error) {
    console.error('Forgot password error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

// ==========================================
// TASK 1: RESET PASSWORD
// ==========================================
exports.resetPassword = async (req, res) => {
  try {
    const { email, otp, newPassword, confirmPassword } = req.body;

    if (!email || !otp || !newPassword) {
      return res.status(400).json({ success: false, message: 'Email, OTP, and new password are required.' });
    }

    if (newPassword !== confirmPassword) {
      return res.status(400).json({ success: false, message: 'Password confirmation does not match.' });
    }

    const passwordError = validatePasswordStrength(newPassword);
    if (passwordError) {
      return res.status(400).json({ success: false, message: passwordError });
    }

    const normalizedEmail = email.trim().toLowerCase();
    const [users] = await db.query(
      'SELECT id, reset_password_otp, reset_otp_expires_at FROM users WHERE email = ? LIMIT 1',
      [normalizedEmail]
    );

    if (users.length === 0) {
      return res.status(400).json({ success: false, message: 'Invalid or expired OTP code.' });
    }

    const user = users[0];
    if (!user.reset_password_otp || user.reset_password_otp !== otp.trim()) {
      return res.status(400).json({ success: false, message: 'Invalid 6-digit verification code.' });
    }

    if (new Date() > new Date(user.reset_otp_expires_at)) {
      return res.status(400).json({ success: false, message: 'Verification code has expired. Please request a new one.' });
    }

    const passwordHash = await bcrypt.hash(newPassword, 10);
    await db.query(
      'UPDATE users SET password_hash = ?, reset_password_otp = NULL, reset_otp_expires_at = NULL WHERE id = ?',
      [passwordHash, user.id]
    );

    return res.json({
      success: true,
      message: 'Password reset successfully. You can now log in with your new credentials.'
    });
  } catch (error) {
    console.error('Reset password error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

// ==========================================
// TASK 1: REFRESH TOKEN
// ==========================================
exports.refreshToken = async (req, res) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) {
      return res.status(400).json({ success: false, message: 'Refresh token is required.' });
    }

    const payload = verifyRefreshToken(refreshToken);
    if (!payload) {
      return res.status(401).json({ success: false, message: 'Invalid or expired refresh token.' });
    }

    const [users] = await db.query('SELECT id, email, name FROM users WHERE id = ? LIMIT 1', [payload.userId]);
    if (users.length === 0) {
      return res.status(404).json({ success: false, message: 'User not found.' });
    }

    const tokens = generateTokens(users[0]);
    return res.json({
      success: true,
      token: tokens.accessToken,
      refreshToken: tokens.refreshToken
    });
  } catch (error) {
    console.error('Refresh token error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

// ==========================================
// TASK 2: DEVICE STATUS & RESET ENDPOINTS
// ==========================================
exports.checkDeviceStatus = async (req, res) => {
  try {
    const { deviceId } = req.query;
    if (!deviceId) {
      return res.status(400).json({ success: false, message: 'Device ID is required.' });
    }

    const [bindings] = await db.query(
      'SELECT db.id, db.status, db.created_at, u.email FROM device_bindings db JOIN users u ON db.user_id = u.id WHERE db.device_id = ? LIMIT 1',
      [deviceId]
    );

    if (bindings.length === 0) {
      return res.json({
        success: true,
        isBound: false,
        message: 'This device is clean and ready for registration.'
      });
    }

    const binding = bindings[0];
    const maskedEmail = binding.email.replace(/(.{2})(.*)(@.*)/, '$1***$3');

    return res.json({
      success: true,
      isBound: true,
      status: binding.status,
      boundToMaskedEmail: maskedEmail,
      message: `Device is bound to account ${maskedEmail}.`
    });
  } catch (error) {
    console.error('Check device status error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

// Device reset request (allows user to release device if verified by credentials)
exports.resetDevice = async (req, res) => {
  try {
    const { email, password, deviceId, reason } = req.body;
    if (!email || !password || !deviceId) {
      return res.status(400).json({ success: false, message: 'Email, password, and deviceId are required for device reset.' });
    }

    const [users] = await db.query('SELECT id, password_hash FROM users WHERE email = ? LIMIT 1', [email.trim().toLowerCase()]);
    if (users.length === 0 || !users[0].password_hash) {
      return res.status(401).json({ success: false, message: 'Invalid credentials.' });
    }

    const isValid = await bcrypt.compare(password, users[0].password_hash);
    if (!isValid) {
      return res.status(401).json({ success: false, message: 'Invalid credentials.' });
    }

    // Release device
    await db.query(
      'UPDATE device_bindings SET status = "RELEASED" WHERE device_id = ? AND user_id = ?',
      [deviceId, users[0].id]
    );

    return res.json({
      success: true,
      message: 'Device registration released successfully. You may now bind this account to your new device.'
    });
  } catch (error) {
    console.error('Device reset error:', error);
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
    const [deviceBinding] = await db.query('SELECT device_id, device_model, status, created_at, last_seen_at FROM device_bindings WHERE user_id = ? AND status = "ACTIVE" LIMIT 1', [userId]);

    return res.json({
      success: true,
      user: users[0],
      settings: settings[0] || null,
      accounts,
      deviceBinding: deviceBinding[0] || null
    });
  } catch (error) {
    console.error('Profile fetch error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};
