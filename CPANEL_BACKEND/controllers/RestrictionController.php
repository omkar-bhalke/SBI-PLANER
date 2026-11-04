<?php
/**
 * App Restrictions & Overspending Enforcement Controller for cPanel
 * Fulfills Task 5 & Accessibility Service tracking
 */

require_once __DIR__ . '/../config/db.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Validator.php';
require_once __DIR__ . '/../middleware/AuthMiddleware.php';

class RestrictionController {

    // GET /api/restrictions
    public static function get(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $db = Database::getConnection();

        $stmt = $db->prepare("SELECT package_name as packageName, app_name as appName, is_enabled as isEnabled FROM app_restrictions WHERE user_id = ?");
        $stmt->execute([$userId]);
        $items = $stmt->fetchAll();

        // If user has not configured restrictions yet, supply default Indian payment apps
        if (empty($items)) {
            $defaults = [
                ['packageName' => 'com.phonepe.app', 'appName' => 'PhonePe', 'isEnabled' => true],
                ['packageName' => 'com.google.android.apps.nbu.paisa.user', 'appName' => 'Google Pay (GPay)', 'isEnabled' => true],
                ['packageName' => 'net.one97.paytm', 'appName' => 'Paytm', 'isEnabled' => true],
                ['packageName' => 'in.org.npci.upiapp', 'appName' => 'BHIM UPI', 'isEnabled' => true],
                ['packageName' => 'com.sbi.upi', 'appName' => 'BHIM SBI Pay', 'isEnabled' => true]
            ];
            Response::success(['restrictions' => $defaults]);
        }

        foreach ($items as &$i) {
            $i['isEnabled'] = (bool)$i['isEnabled'];
        }

        Response::success(['restrictions' => $items]);
    }

    // POST /api/restrictions
    public static function update(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $body = Validator::getJsonBody();

        $restrictions = $body['restrictions'] ?? [];
        if (!is_array($restrictions)) {
            Response::error('Invalid restrictions array format', 400);
        }

        $db = Database::getConnection();
        $db->beginTransaction();

        try {
            $del = $db->prepare("DELETE FROM app_restrictions WHERE user_id = ?");
            $del->execute([$userId]);

            $ins = $db->prepare("INSERT INTO app_restrictions (id, user_id, package_name, app_name, is_enabled) VALUES (?, ?, ?, ?, ?)");
            foreach ($restrictions as $r) {
                $pkg = Validator::sanitizeString($r['packageName'] ?? '');
                $app = Validator::sanitizeString($r['appName'] ?? $pkg);
                $enabled = !empty($r['isEnabled']) ? 1 : 0;
                if (!empty($pkg)) {
                    $ins->execute(['res_' . bin2hex(random_bytes(8)), $userId, $pkg, $app, $enabled]);
                }
            }

            $db->commit();
        } catch (Exception $e) {
            $db->rollBack();
            Response::error('Failed to update restrictions: ' . $e->getMessage(), 500);
        }

        Response::success(null, 'Payment app restrictions updated successfully in cPanel database.');
    }

    // POST /api/restrictions/log-event
    public static function logEvent(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $body = Validator::getJsonBody();

        $packageName = Validator::sanitizeString($body['packageName'] ?? '');
        $appName = Validator::sanitizeString($body['appName'] ?? $packageName);
        $dailyLimit = floatval($body['dailyLimit'] ?? 0);
        $amountSpent = floatval($body['amountSpent'] ?? 0);
        $amountExceeded = floatval($body['amountExceeded'] ?? 0);
        $actionTaken = strtoupper(trim($body['actionTaken'] ?? 'BLOCKED'));

        if (empty($packageName)) {
            Response::error('Package name is required', 400);
        }

        $db = Database::getConnection();
        $id = 'evt_' . bin2hex(random_bytes(8));
        $stmt = $db->prepare("INSERT INTO app_restriction_events (id, user_id, package_name, app_name, daily_limit, amount_spent, amount_exceeded, action_taken) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([$id, $userId, $packageName, $appName, $dailyLimit, $amountSpent, $amountExceeded, $actionTaken]);

        Response::success(['eventId' => $id], 'Restriction event logged successfully.');
    }
}
