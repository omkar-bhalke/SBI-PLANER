<?php
/**
 * User Financial Settings & Custom Categories Controller for cPanel
 */

require_once __DIR__ . '/../config/db.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Validator.php';
require_once __DIR__ . '/../middleware/AuthMiddleware.php';

class SettingsController {

    // GET /api/settings
    public static function get(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $db = Database::getConnection();

        $stmt = $db->prepare("SELECT daily_spending_limit, selected_bank, currency, currency_symbol, theme, ai_provider, onboarding_completed, accessibility_enabled FROM user_settings WHERE user_id = ? LIMIT 1");
        $stmt->execute([$userId]);
        $settings = $stmt->fetch() ?: [];

        Response::success($settings);
    }

    // PUT /api/settings/daily-limit
    public static function updateDailyLimit(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $body = Validator::getJsonBody();

        $dailyLimit = floatval($body['dailyLimit'] ?? 0);
        if ($dailyLimit <= 0) {
            Response::error('Daily limit must be greater than zero', 400);
        }

        $db = Database::getConnection();
        $stmt = $db->prepare("UPDATE user_settings SET daily_spending_limit = ?, updated_at = NOW() WHERE user_id = ?");
        $stmt->execute([$dailyLimit, $userId]);

        Response::success(['dailyLimit' => $dailyLimit], 'Daily spending limit updated.');
    }

    // PUT /api/settings/initial-balance
    public static function updateInitialBalance(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $body = Validator::getJsonBody();

        $balance = floatval($body['initialBalance'] ?? 0);

        $db = Database::getConnection();
        $db->beginTransaction();

        try {
            // Fetch total income and expense to recalculate current balance correctly
            $tot = $db->prepare("SELECT 
                COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END), 0) -
                COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END), 0) as netCashflow
                FROM transactions WHERE user_id = ?");
            $tot->execute([$userId]);
            $net = floatval($tot->fetch()['netCashflow']);

            $newCurrent = $balance + $net;

            $upd = $db->prepare("UPDATE accounts SET opening_balance = ?, current_balance = ?, updated_at = NOW() WHERE user_id = ? AND is_primary = 1");
            $upd->execute([$balance, $newCurrent, $userId]);

            $db->commit();
        } catch (Exception $e) {
            $db->rollBack();
            Response::error('Failed to update balance: ' . $e->getMessage(), 500);
        }

        Response::success(['initialBalance' => $balance, 'currentBalance' => $newCurrent], 'Opening balance updated.');
    }

    // PUT /api/settings/bank
    public static function updateBank(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $body = Validator::getJsonBody();

        $bank = Validator::sanitizeString($body['bank'] ?? 'State Bank of India (SBI)');

        $db = Database::getConnection();
        $stmt = $db->prepare("UPDATE user_settings SET selected_bank = ?, updated_at = NOW() WHERE user_id = ?");
        $stmt->execute([$bank, $userId]);

        $accStmt = $db->prepare("UPDATE accounts SET bank_name = ? WHERE user_id = ? AND is_primary = 1");
        $accStmt->execute([$bank, $userId]);

        Response::success(['bank' => $bank], 'Primary bank updated.');
    }

    // GET /api/categories
    public static function listCategories(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $db = Database::getConnection();

        $stmt = $db->prepare("SELECT id, category_name as name, category_type as type, icon_name as iconName, color_hex as colorHex, is_default as isDefault FROM categories WHERE user_id = ? OR is_default = 1 ORDER BY is_default DESC, category_name ASC");
        $stmt->execute([$userId]);
        $categories = $stmt->fetchAll();

        foreach ($categories as &$c) {
            $c['isDefault'] = (bool)$c['isDefault'];
        }

        Response::success(['categories' => $categories]);
    }

    // POST /api/categories
    public static function addCategory(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $body = Validator::getJsonBody();

        $name = Validator::sanitizeString($body['name'] ?? '');
        $type = strtoupper(trim($body['type'] ?? 'EXPENSE'));
        $colorHex = Validator::sanitizeString($body['colorHex'] ?? '#1E3E62');
        $iconName = Validator::sanitizeString($body['iconName'] ?? 'category');

        if (empty($name)) {
            Response::error('Category name is required', 400);
        }
        if (!in_array($type, ['INCOME', 'EXPENSE'])) {
            $type = 'EXPENSE';
        }

        $id = 'cat_' . bin2hex(random_bytes(8));
        $db = Database::getConnection();
        $stmt = $db->prepare("INSERT INTO categories (id, user_id, category_name, category_type, icon_name, color_hex, is_default) VALUES (?, ?, ?, ?, ?, ?, 0)");
        $stmt->execute([$id, $userId, $name, $type, $iconName, $colorHex]);

        Response::success([
            'category' => [
                'id'        => $id,
                'name'      => $name,
                'type'      => $type,
                'iconName'  => $iconName,
                'colorHex'  => $colorHex,
                'isDefault' => false
            ]
        ], 'Custom category added.');
    }

    // DELETE /api/categories/{id}
    public static function deleteCategory(string $id): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $db = Database::getConnection();

        $stmt = $db->prepare("DELETE FROM categories WHERE id = ? AND user_id = ? AND is_default = 0");
        $stmt->execute([$id, $userId]);

        Response::success(null, 'Category deleted.');
    }
}
