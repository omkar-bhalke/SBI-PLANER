<?php
/**
 * Authentication and Device-Binding Verification Middleware
 */

require_once __DIR__ . '/../helpers/Jwt.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../config/db.php';

class AuthMiddleware {
    public static function authenticate(): array {
        $headers = self::getRequestHeaders();

        // 1. Extract Bearer token
        $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
        if (empty($authHeader) || !preg_match('/Bearer\s(\S+)/i', $authHeader, $matches)) {
            Response::unauthorized('Authentication token missing or invalid', 'TOKEN_MISSING');
        }

        $token = $matches[1];
        $payload = Jwt::verifyToken($token);
        if (!$payload || !isset($payload['userId'])) {
            Response::unauthorized('Invalid or expired authentication session. Please login again.', 'TOKEN_EXPIRED');
        }

        $userId = $payload['userId'];
        $db = Database::getConnection();

        // 2. Fetch user from cPanel MySQL database
        $stmt = $db->prepare("SELECT id, email, name, profile_image FROM users WHERE id = ? LIMIT 1");
        $stmt->execute([$userId]);
        $user = $stmt->fetch();

        if (!$user) {
            Response::unauthorized('User account no longer exists', 'USER_NOT_FOUND');
        }

        // 3. Strict Device-Binding Enforcement (Task 2 & Requirement 3)
        $deviceId = $headers['X-Device-Id'] ?? $headers['x-device-id'] ?? '';
        if (!empty($deviceId)) {
            $devStmt = $db->prepare("SELECT user_id, status FROM device_bindings WHERE device_id = ? LIMIT 1");
            $devStmt->execute([$deviceId]);
            $binding = $devStmt->fetch();

            if ($binding) {
                if ($binding['user_id'] !== $userId) {
                    Response::error('This device is registered to another user account. Only one account per device is permitted.', 403, 'DEVICE_BOUND_TO_ANOTHER_ACCOUNT');
                }
                if ($binding['status'] === 'SUSPENDED') {
                    Response::error('This device has been temporarily suspended by system administrator.', 403, 'DEVICE_SUSPENDED');
                }
            }
        }

        return [
            'user'     => $user,
            'deviceId' => $deviceId
        ];
    }

    private static function getRequestHeaders(): array {
        if (function_exists('getallheaders')) {
            $headers = getallheaders();
            if ($headers !== false) {
                return $headers;
            }
        }

        $headers = [];
        foreach ($_SERVER as $name => $value) {
            if (substr($name, 0, 5) === 'HTTP_') {
                $headerName = str_replace(' ', '-', ucwords(strtolower(str_replace('_', ' ', substr($name, 5)))));
                $headers[$headerName] = $value;
            }
        }

        if (isset($_SERVER['CONTENT_TYPE'])) $headers['Content-Type'] = $_SERVER['CONTENT_TYPE'];
        if (isset($_SERVER['HTTP_AUTHORIZATION'])) $headers['Authorization'] = $_SERVER['HTTP_AUTHORIZATION'];
        if (isset($_SERVER['REDIRECT_HTTP_AUTHORIZATION'])) $headers['Authorization'] = $_SERVER['REDIRECT_HTTP_AUTHORIZATION'];

        return $headers;
    }
}
