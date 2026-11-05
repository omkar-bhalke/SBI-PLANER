<?php
/**
 * Balance Controller for Real Bank Balance and Statement History
 * Target Domain: https://radhewins.buzz
 */

require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../middleware/AuthMiddleware.php';
require_once __DIR__ . '/../config/db.php';

class BalanceController {

    /**
     * Get Current Real Bank Balance & Account Summary
     * GET /api/balance/current
     */
    public static function current(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $db = Database::getConnection();

        // 1. Fetch primary account
        $stmt = $db->prepare("SELECT * FROM accounts WHERE user_id = ? AND is_primary = 1 LIMIT 1");
        $stmt->execute([$userId]);
        $account = $stmt->fetch();

        // 2. Fetch latest balance history entry
        $bhStmt = $db->prepare("
            SELECT * FROM balance_history 
            WHERE user_id = ? 
            ORDER BY created_at DESC 
            LIMIT 1
        ");
        $bhStmt->execute([$userId]);
        $latestHistory = $bhStmt->fetch();

        Response::success([
            'currentBalance'     => $account ? (float)$account['current_balance'] : 0.0,
            'openingBalance'     => $account ? (float)$account['opening_balance'] : 0.0,
            'accountNumber'      => $latestHistory['account_number'] ?? 'xxx188',
            'bankName'           => $account['bank_name'] ?? 'State Bank of India (SBI)',
            'lastStatementDate'  => $latestHistory['raw_statement_date'] ?? null,
            'lastStatementSync'  => $latestHistory['created_at'] ?? null,
            'rawStatementText'   => $latestHistory['statement_text'] ?? null
        ]);
    }

    /**
     * Get Recent Bank Balance History from Statements
     * GET /api/balance/history
     */
    public static function history(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $limit = isset($_GET['limit']) ? min(100, max(1, (int)$_GET['limit'])) : 20;

        $db = Database::getConnection();
        $stmt = $db->prepare("
            SELECT id, account_number AS accountNumber, balance, raw_statement_date AS rawStatementDate, source, created_at AS createdAt
            FROM balance_history
            WHERE user_id = ?
            ORDER BY created_at DESC
            LIMIT ?
        ");
        $stmt->bindValue(1, $userId);
        $stmt->bindValue(2, $limit, PDO::PARAM_INT);
        $stmt->execute();
        $records = $stmt->fetchAll();

        foreach ($records as &$r) {
            $r['balance'] = (float)$r['balance'];
        }

        Response::success([
            'count'   => count($records),
            'history' => $records
        ]);
    }
}
