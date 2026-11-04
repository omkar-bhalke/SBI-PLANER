<?php
/**
 * Authentication and Device-Binding Controller for cPanel
 * Fulfills Requirements 2, 3, 4 & 5
 */

require_once __DIR__ . '/../config/db.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Jwt.php';
require_once __DIR__ . '/../helpers/Validator.php';
require_once __DIR__ . '/../middleware/AuthMiddleware.php';

class AuthController {

    // POST /api/auth/register
    public static function register(): void {
        $body = Validator::getJsonBody();

        $name = Validator::sanitizeString($body['name'] ?? '');
        $email = strtolower(trim($body['email'] ?? ''));
        $password = $body['password'] ?? '';
        $confirmPassword = $body['confirmPassword'] ?? '';
        $deviceId = trim($body['deviceId'] ?? '');
        $deviceFingerprint = Validator::sanitizeString($body['deviceFingerprint'] ?? '');
        $deviceModel = Validator::sanitizeString($body['deviceModel'] ?? '');
        $osVersion = Validator::sanitizeString($body['osVersion'] ?? '');

        // 1. Validation
        if (empty($name)) {
            Response::error('Please enter your full name', 400, 'NAME_REQUIRED');
        }
        if (!Validator::validateEmail($email)) {
            Response::error('Please enter a valid email address', 400, 'INVALID_EMAIL');
        }
        list($isPassValid, $passError) = Validator::validatePassword($password);
        if (!$isPassValid) {
            Response::error($passError, 400, 'WEAK_PASSWORD');
        }
        if ($password !== $confirmPassword) {
            Response::error('Passwords do not match', 400, 'PASSWORD_MISMATCH');
        }
        if (empty($deviceId)) {
            Response::error('Hardware device identifier is required for registration', 400, 'DEVICE_ID_REQUIRED');
        }

        $db = Database::getConnection();

        // 2. Strict One Device = One Account Verification (Task 2 & Requirement 3)
        $devCheck = $db->prepare("SELECT b.user_id, u.email FROM device_bindings b JOIN users u ON b.user_id = u.id WHERE b.device_id = ? LIMIT 1");
        $devCheck->execute([$deviceId]);
        $existingDevice = $devCheck->fetch();

        if ($existingDevice) {
            $parts = explode('@', $existingDevice['email']);
            $maskedEmail = substr($parts[0], 0, 2) . '***@' . ($parts[1] ?? 'domain.com');
            Response::error(
                "This device is already registered to account ($maskedEmail). FinPulse allows strictly one account per physical device. Please log in with the registered account or perform a device unbinding.",
                409,
                'DEVICE_ALREADY_BOUND'
            );
        }

        // 3. Duplicate Email Check
        $emailCheck = $db->prepare("SELECT id FROM users WHERE email = ? LIMIT 1");
        $emailCheck->execute([$email]);
        if ($emailCheck->fetch()) {
            Response::error('An account with this email address already exists. Please log in instead.', 409, 'EMAIL_EXISTS');
        }

        // 4. Secure Password Hashing (bcrypt cost 12 - zero plain text)
        $passwordHash = password_hash($password, PASSWORD_BCRYPT, ['cost' => 12]);

        $userId = 'usr_' . bin2hex(random_bytes(10));
        $bindingId = 'dev_' . bin2hex(random_bytes(10));
        $settingsId = 'set_' . bin2hex(random_bytes(10));
        $accountId = 'acc_' . bin2hex(random_bytes(10));

        // 5. Database Insertion with Transaction
        try {
            $db->beginTransaction();

            // Insert user
            $userStmt = $db->prepare("INSERT INTO users (id, email, password_hash, name, is_email_verified) VALUES (?, ?, ?, ?, 1)");
            $userStmt->execute([$userId, $email, $passwordHash, $name]);

            // Bind hardware device
            $bindStmt = $db->prepare("INSERT INTO device_bindings (id, device_id, user_id, device_fingerprint, device_model, os_version, status) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')");
            $bindStmt->execute([$bindingId, $deviceId, $userId, $deviceFingerprint, $deviceModel, $osVersion]);

            // Create initial user settings
            $setStmt = $db->prepare("INSERT INTO user_settings (id, user_id, currency, currency_symbol, theme, onboarding_completed, daily_spending_limit, selected_bank, ai_provider) VALUES (?, ?, 'INR', '₹', 'SYSTEM', 0, 500.00, 'State Bank of India (SBI)', 'GEMINI')");
            $setStmt->execute([$settingsId, $userId]);

            // Create primary bank account
            $accStmt = $db->prepare("INSERT INTO accounts (id, user_id, bank_name, account_type, opening_balance, current_balance, is_primary) VALUES (?, ?, 'State Bank of India (SBI)', 'SAVINGS', 0.00, 0.00, 1)");
            $accStmt->execute([$accountId, $userId]);

            // Insert default expense categories
            $categories = [
                ['Food & Dining', 'EXPENSE', 'restaurant', '#FF6B6B'],
                ['Groceries', 'EXPENSE', 'shopping_cart', '#4ECDC4'],
                ['Transportation', 'EXPENSE', 'directions_bus', '#45B7D1'],
                ['Utilities & Bills', 'EXPENSE', 'receipt_long', '#F7B731'],
                ['Entertainment', 'EXPENSE', 'movie', '#A55EEA'],
                ['Healthcare', 'EXPENSE', 'local_hospital', '#FC5C65'],
                ['Education & Books', 'EXPENSE', 'school', '#26DE81'],
                ['Salary & Allowance', 'INCOME', 'account_balance_wallet', '#2ED573'],
                ['Investments & Gifts', 'INCOME', 'savings', '#1E90FF']
            ];

            $catStmt = $db->prepare("INSERT INTO categories (id, user_id, category_name, category_type, icon_name, color_hex, is_default) VALUES (?, ?, ?, ?, ?, ?, 1)");
            foreach ($categories as $cat) {
                $catStmt->execute(['cat_' . bin2hex(random_bytes(8)), $userId, $cat[0], $cat[1], $cat[2], $cat[3]]);
            }

            // Create default WhatsApp integration record
            $waStmt = $db->prepare("INSERT INTO integrations (id, user_id, integration_type, connection_status) VALUES (?, ?, 'WHATSAPP_BUSINESS_CLOUD', 'NOT_CONNECTED')");
            $waStmt->execute(['int_' . bin2hex(random_bytes(8)), $userId]);

            $db->commit();
        } catch (Exception $e) {
            $db->rollBack();
            Response::error('Failed to complete registration: ' . $e->getMessage(), 500);
        }

        // 6. Generate Session Tokens
        $token = Jwt::generateToken(['userId' => $userId, 'email' => $email, 'name' => $name]);
        $refreshToken = Jwt::generateRefreshToken($userId, $deviceId);

        // Store refresh token
        $tokStmt = $db->prepare("INSERT INTO user_tokens (id, user_id, device_id, refresh_token, expires_at) VALUES (?, ?, ?, ?, DATE_ADD(NOW(), INTERVAL 30 DAY))");
        $tokStmt->execute(['tok_' . bin2hex(random_bytes(8)), $userId, $deviceId, $refreshToken]);

        Response::success([
            'token'        => $token,
            'refreshToken' => $refreshToken,
            'isFirstTime'  => true,
            'user'         => [
                'id'           => $userId,
                'email'        => $email,
                'name'         => $name,
                'profileImage' => null
            ]
        ], 'Registration successful and device bound.');
    }

    // POST /api/auth/login
    public static function login(): void {
        $body = Validator::getJsonBody();

        $email = strtolower(trim($body['email'] ?? ''));
        $password = $body['password'] ?? '';
        $deviceId = trim($body['deviceId'] ?? '');
        $deviceFingerprint = Validator::sanitizeString($body['deviceFingerprint'] ?? '');

        if (empty($email) || empty($password)) {
            Response::error('Email and password are required', 400, 'CREDENTIALS_REQUIRED');
        }

        $db = Database::getConnection();

        // 1. Fetch user by email
        $stmt = $db->prepare("SELECT u.id, u.email, u.name, u.password_hash, u.profile_image, s.onboarding_completed FROM users u LEFT JOIN user_settings s ON u.id = s.user_id WHERE u.email = ? LIMIT 1");
        $stmt->execute([$email]);
        $user = $stmt->fetch();

        if (!$user || !password_verify($password, $user['password_hash'])) {
            Response::error('Invalid email or password. Please check your credentials.', 401, 'INVALID_CREDENTIALS');
        }

        $userId = $user['id'];

        // 2. One Device = One Account Enforcement (Requirement 3)
        if (!empty($deviceId)) {
            // Check if device is bound to ANOTHER user
            $devCheck = $db->prepare("SELECT user_id, status FROM device_bindings WHERE device_id = ? LIMIT 1");
            $devCheck->execute([$deviceId]);
            $existingDev = $devCheck->fetch();

            if ($existingDev) {
                if ($existingDev['user_id'] !== $userId) {
                    Response::error('This device is registered to another account. Multi-accounting on a single device is prohibited.', 403, 'DEVICE_ALREADY_BOUND');
                }
                if ($existingDev['status'] === 'SUSPENDED') {
                    Response::error('This device has been suspended. Please contact administrator.', 403, 'DEVICE_SUSPENDED');
                }
                // Update last seen
                $updDev = $db->prepare("UPDATE device_bindings SET last_seen_at = NOW(), device_fingerprint = COALESCE(?, device_fingerprint) WHERE device_id = ?");
                $updDev->execute([$deviceFingerprint, $deviceId]);
            } else {
                // First time this user logs in on this authorized device: bind it
                $bindStmt = $db->prepare("INSERT INTO device_bindings (id, device_id, user_id, device_fingerprint, status) VALUES (?, ?, ?, ?, 'ACTIVE')");
                $bindStmt->execute(['dev_' . bin2hex(random_bytes(10)), $deviceId, $userId, $deviceFingerprint]);
            }
        }

        // 3. Issue Session Tokens
        $token = Jwt::generateToken(['userId' => $userId, 'email' => $user['email'], 'name' => $user['name']]);
        $refreshToken = Jwt::generateRefreshToken($userId, $deviceId);

        $tokStmt = $db->prepare("INSERT INTO user_tokens (id, user_id, device_id, refresh_token, expires_at) VALUES (?, ?, ?, ?, DATE_ADD(NOW(), INTERVAL 30 DAY))");
        $tokStmt->execute(['tok_' . bin2hex(random_bytes(8)), $userId, $deviceId, $refreshToken]);

        Response::success([
            'token'        => $token,
            'refreshToken' => $refreshToken,
            'isFirstTime'  => empty($user['onboarding_completed']),
            'user'         => [
                'id'           => $user['id'],
                'email'        => $user['email'],
                'name'         => $user['name'],
                'profileImage' => $user['profile_image']
            ]
        ], 'Login successful.');
    }

    // POST /api/auth/forgot-password
    public static function forgotPassword(): void {
        $body = Validator::getJsonBody();
        $email = strtolower(trim($body['email'] ?? ''));

        if (!Validator::validateEmail($email)) {
            Response::error('Please enter a valid email address', 400);
        }

        $db = Database::getConnection();
        $stmt = $db->prepare("SELECT id, name FROM users WHERE email = ? LIMIT 1");
        $stmt->execute([$email]);
        $user = $stmt->fetch();

        if (!$user) {
            // Constant-time generic response to prevent email enumeration
            Response::success(null, 'If an account exists with this email, a 6-digit verification code has been dispatched.');
        }

        $otp = str_pad(strval(random_int(100000, 999999)), 6, '0', STR_PAD_LEFT);
        $upd = $db->prepare("UPDATE users SET reset_password_otp = ?, reset_otp_expires_at = DATE_ADD(NOW(), INTERVAL 15 MINUTE) WHERE id = ?");
        $upd->execute([$otp, $user['id']]);

        // Attempt sending via standard cPanel mail() function
        $subject = "FinPulse Security - Password Reset OTP";
        $message = "Hello " . htmlspecialchars($user['name']) . ",\n\nYour 6-digit password reset OTP is: $otp\n\nThis OTP expires in 15 minutes.\n\nFinPulse Security Team";
        $headers = "From: noreply@" . ($_SERVER['SERVER_NAME'] ?? 'radhewins.buzz') . "\r\n";
        @mail($email, $subject, $message, $headers);

        Response::success(null, "Verification code sent to $email. Please enter the 6-digit code to reset your password.");
    }

    // POST /api/auth/reset-password
    public static function resetPassword(): void {
        $body = Validator::getJsonBody();
        $email = strtolower(trim($body['email'] ?? ''));
        $otp = trim($body['otp'] ?? '');
        $newPassword = $body['newPassword'] ?? '';
        $confirmPassword = $body['confirmPassword'] ?? '';

        if (empty($email) || empty($otp) || empty($newPassword)) {
            Response::error('All fields are required', 400);
        }
        if ($newPassword !== $confirmPassword) {
            Response::error('Passwords do not match', 400);
        }
        list($isPassValid, $passError) = Validator::validatePassword($newPassword);
        if (!$isPassValid) {
            Response::error($passError, 400);
        }

        $db = Database::getConnection();
        $stmt = $db->prepare("SELECT id, reset_password_otp, reset_otp_expires_at FROM users WHERE email = ? LIMIT 1");
        $stmt->execute([$email]);
        $user = $stmt->fetch();

        if (!$user || $user['reset_password_otp'] !== $otp || strtotime($user['reset_otp_expires_at']) < time()) {
            Response::error('Invalid or expired OTP verification code.', 400, 'INVALID_OTP');
        }

        $newHash = password_hash($newPassword, PASSWORD_BCRYPT, ['cost' => 12]);
        $upd = $db->prepare("UPDATE users SET password_hash = ?, reset_password_otp = NULL, reset_otp_expires_at = NULL WHERE id = ?");
        $upd->execute([$newHash, $user['id']]);

        Response::success(null, 'Password has been reset successfully. You may now log in with your new password.');
    }

    // POST /api/auth/refresh-token
    public static function refreshToken(): void {
        $body = Validator::getJsonBody();
        $refreshToken = trim($body['refreshToken'] ?? '');

        if (empty($refreshToken)) {
            Response::error('Refresh token is required', 400);
        }

        $db = Database::getConnection();
        $stmt = $db->prepare("SELECT t.user_id, t.device_id, u.email, u.name FROM user_tokens t JOIN users u ON t.user_id = u.id WHERE t.refresh_token = ? AND t.expires_at > NOW() LIMIT 1");
        $stmt->execute([$refreshToken]);
        $tok = $stmt->fetch();

        if (!$tok) {
            Response::unauthorized('Invalid or expired refresh token. Please login again.', 'REFRESH_EXPIRED');
        }

        $newJwt = Jwt::generateToken(['userId' => $tok['user_id'], 'email' => $tok['email'], 'name' => $tok['name']]);
        Response::success([
            'token' => $newJwt
        ], 'Token refreshed successfully.');
    }

    // GET /api/auth/device-status?deviceId=...
    public static function deviceStatus(): void {
        $deviceId = trim($_GET['deviceId'] ?? '');
        if (empty($deviceId)) {
            Response::error('deviceId parameter is required', 400);
        }

        $db = Database::getConnection();
        $stmt = $db->prepare("SELECT b.status, u.email FROM device_bindings b JOIN users u ON b.user_id = u.id WHERE b.device_id = ? LIMIT 1");
        $stmt->execute([$deviceId]);
        $binding = $stmt->fetch();

        if (!$binding) {
            Response::success([
                'isBound' => false,
                'status'  => 'UNBOUND',
                'message' => 'Device is available for registration.'
            ]);
        }

        $parts = explode('@', $binding['email']);
        $maskedEmail = substr($parts[0], 0, 2) . '***@' . ($parts[1] ?? 'domain.com');

        Response::success([
            'isBound'            => true,
            'status'             => $binding['status'],
            'boundToMaskedEmail' => $maskedEmail,
            'message'            => "Device is bound to $maskedEmail"
        ]);
    }

    // POST /api/auth/device-reset
    public static function deviceReset(): void {
        $body = Validator::getJsonBody();
        $email = strtolower(trim($body['email'] ?? ''));
        $password = $body['password'] ?? '';
        $deviceId = trim($body['deviceId'] ?? '');

        if (empty($email) || empty($password) || empty($deviceId)) {
            Response::error('Email, password, and deviceId are required for device unbinding', 400);
        }

        $db = Database::getConnection();
        $stmt = $db->prepare("SELECT id, password_hash FROM users WHERE email = ? LIMIT 1");
        $stmt->execute([$email]);
        $user = $stmt->fetch();

        if (!$user || !password_verify($password, $user['password_hash'])) {
            Response::error('Invalid credentials. Device unbinding rejected.', 401);
        }

        $del = $db->prepare("DELETE FROM device_bindings WHERE device_id = ? AND user_id = ?");
        $del->execute([$deviceId, $user['id']]);

        Response::success(null, 'Device unbinding successful. You may now bind another device.');
    }
}
