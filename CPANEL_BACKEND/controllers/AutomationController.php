<?php
/**
 * Automation Settings and Audit Logging Controller
 * Target Domain: https://radhewins.buzz
 */

require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../middleware/AuthMiddleware.php';
require_once __DIR__ . '/../config/db.php';

class AutomationController {

    /**
     * Get User Automation Config
     * GET /api/automation/config
     */
    public static function getConfig(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $db = Database::getConnection();

        $stmt = $db->prepare("SELECT * FROM automation_settings WHERE user_id = ? LIMIT 1");
        $stmt->execute([$userId]);
        $cfg = $stmt->fetch();

        if (!$cfg) {
            // Create default settings row
            $newId = 'auto_' . bin2hex(random_bytes(16));
            $ins = $db->prepare("
                INSERT INTO automation_settings (id, user_id, sbi_target_phone, interval_minutes, is_enabled, last_status, created_at)
                VALUES (?, ?, '919022690226', 30, 1, 'IDLE', NOW())
            ");
            $ins->execute([$newId, $userId]);
            $cfg = [
                'id' => $newId,
                'user_id' => $userId,
                'sbi_target_phone' => '919022690226',
                'interval_minutes' => 30,
                'is_enabled' => 1,
                'railway_api_url' => null,
                'railway_api_token' => null,
                'last_run_at' => null,
                'last_status' => 'IDLE'
            ];
        }

        Response::success([
            'sbiTargetPhone'  => $cfg['sbi_target_phone'],
            'intervalMinutes' => (int)$cfg['interval_minutes'],
            'isEnabled'       => (bool)$cfg['is_enabled'],
            'railwayApiUrl'   => $cfg['railway_api_url'],
            'lastRunAt'       => $cfg['last_run_at'],
            'lastStatus'      => $cfg['last_status']
        ]);
    }

    /**
     * Update User Automation Config
     * POST /api/automation/config
     */
    public static function updateConfig(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $input = json_decode(file_get_contents('php://input'), true);

        if (!$input) {
            Response::error('Invalid JSON payload', 400);
        }

        $sbiTargetPhone = trim($input['sbiTargetPhone'] ?? $input['sbi_target_phone'] ?? '919022690226');
        $intervalMinutes = isset($input['intervalMinutes']) ? max(5, min(1440, (int)$input['intervalMinutes'])) : 30;
        $isEnabled = isset($input['isEnabled']) ? ($input['isEnabled'] ? 1 : 0) : 1;
        $railwayApiUrl = trim($input['railwayApiUrl'] ?? $input['railway_api_url'] ?? '');

        $db = Database::getConnection();

        // Check if exists
        $check = $db->prepare("SELECT id FROM automation_settings WHERE user_id = ? LIMIT 1");
        $check->execute([$userId]);
        $exists = $check->fetch();

        if ($exists) {
            $stmt = $db->prepare("
                UPDATE automation_settings 
                SET sbi_target_phone = ?, interval_minutes = ?, is_enabled = ?, railway_api_url = ?, updated_at = NOW()
                WHERE user_id = ?
            ");
            $stmt->execute([$sbiTargetPhone, $intervalMinutes, $isEnabled, $railwayApiUrl ?: null, $userId]);
        } else {
            $newId = 'auto_' . bin2hex(random_bytes(16));
            $stmt = $db->prepare("
                INSERT INTO automation_settings 
                (id, user_id, sbi_target_phone, interval_minutes, is_enabled, railway_api_url, last_status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, 'CONFIGURED', NOW())
            ");
            $stmt->execute([$newId, $userId, $sbiTargetPhone, $intervalMinutes, $isEnabled, $railwayApiUrl ?: null]);
        }

        Response::success([
            'sbiTargetPhone'  => $sbiTargetPhone,
            'intervalMinutes' => $intervalMinutes,
            'isEnabled'       => (bool)$isEnabled,
            'railwayApiUrl'   => $railwayApiUrl
        ], 'Automation settings updated.');
    }

    /**
     * Log Automation Event
     * POST /api/automation/log
     */
    public static function logEvent(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $input = json_decode(file_get_contents('php://input'), true);

        $eventType = trim($input['eventType'] ?? $input['event_type'] ?? 'GENERAL');
        $message = trim($input['message'] ?? 'Automation log');
        $details = isset($input['details']) ? (is_string($input['details']) ? $input['details'] : json_encode($input['details'])) : null;

        $db = Database::getConnection();
        $logId = 'log_' . bin2hex(random_bytes(16));
        $stmt = $db->prepare("
            INSERT INTO automation_logs (id, user_id, event_type, message, details, created_at)
            VALUES (?, ?, ?, ?, ?, NOW())
        ");
        $stmt->execute([$logId, $userId, $eventType, $message, $details]);

        Response::success(['logId' => $logId], 'Log stored.');
    }

    /**
     * Get Recent Automation Logs
     * GET /api/automation/logs
     */
    public static function getLogs(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $limit = isset($_GET['limit']) ? min(100, max(1, (int)$_GET['limit'])) : 50;

        $db = Database::getConnection();
        $stmt = $db->prepare("
            SELECT id, event_type AS eventType, message, details, created_at AS createdAt
            FROM automation_logs
            WHERE user_id = ?
            ORDER BY created_at DESC
            LIMIT ?
        ");
        $stmt->bindValue(1, $userId);
        $stmt->bindValue(2, $limit, PDO::PARAM_INT);
        $stmt->execute();
        $logs = $stmt->fetchAll();

        Response::success([
            'count' => count($logs),
            'logs'  => $logs
        ]);
    }
}
