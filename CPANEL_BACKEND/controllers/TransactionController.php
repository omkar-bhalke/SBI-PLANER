<?php
/**
 * Transaction and Cloud Synchronization Controller for cPanel
 * Fulfills Requirements 4 & 5 (Permanent storage and cloud restore)
 */

require_once __DIR__ . '/../config/db.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../helpers/Validator.php';
require_once __DIR__ . '/../middleware/AuthMiddleware.php';

class TransactionController {

    // GET /api/transactions
    public static function list(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $db = Database::getConnection();

        $page = max(1, intval($_GET['page'] ?? 1));
        $limit = min(100, max(1, intval($_GET['limit'] ?? 50)));
        $offset = ($page - 1) * $limit;

        $stmt = $db->prepare("SELECT id, amount, transaction_type as type, description, category_name as categoryName, category_id as categoryId, transaction_date as transactionDate, transaction_time as transactionTime, transaction_timestamp as timestamp, notes, idempotency_key as idempotencyKey, client_local_id as clientLocalId FROM transactions WHERE user_id = ? ORDER BY transaction_date DESC, transaction_time DESC LIMIT ? OFFSET ?");
        $stmt->bindValue(1, $userId, PDO::PARAM_STR);
        $stmt->bindValue(2, $limit, PDO::PARAM_INT);
        $stmt->bindValue(3, $offset, PDO::PARAM_INT);
        $stmt->execute();
        $items = $stmt->fetchAll();

        // Convert types
        foreach ($items as &$item) {
            $item['amount'] = floatval($item['amount']);
            $item['timestamp'] = intval($item['timestamp']);
        }

        Response::success([
            'transactions' => $items,
            'page'         => $page,
            'limit'        => $limit
        ]);
    }

    // POST /api/transactions
    public static function create(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $body = Validator::getJsonBody();

        $amount = floatval($body['amount'] ?? 0);
        $type = strtoupper(trim($body['type'] ?? 'EXPENSE'));
        $description = Validator::sanitizeString($body['description'] ?? '');
        $categoryName = Validator::sanitizeString($body['categoryName'] ?? 'General');
        $categoryId = Validator::sanitizeString($body['categoryId'] ?? null);
        $transactionDate = trim($body['transactionDate'] ?? date('Y-m-d'));
        $transactionTime = trim($body['transactionTime'] ?? date('H:i:s'));
        $timestamp = intval($body['timestamp'] ?? (time() * 1000));
        $notes = Validator::sanitizeString($body['notes'] ?? null);
        $idempotencyKey = trim($body['idempotencyKey'] ?? ('tx_' . bin2hex(random_bytes(12))));
        $clientLocalId = Validator::sanitizeString($body['clientLocalId'] ?? null);

        if ($amount <= 0) {
            Response::error('Transaction amount must be greater than zero', 400);
        }
        if (empty($description)) {
            Response::error('Description is required', 400);
        }
        if (!in_array($type, ['INCOME', 'EXPENSE'])) {
            $type = 'EXPENSE';
        }

        $db = Database::getConnection();

        // Idempotency check: if key already exists, return existing record
        $idem = $db->prepare("SELECT id FROM transactions WHERE idempotency_key = ? AND user_id = ? LIMIT 1");
        $idem->execute([$idempotencyKey, $userId]);
        $existing = $idem->fetch();

        if ($existing) {
            Response::success([
                'transaction' => ['id' => $existing['id'], 'amount' => $amount, 'description' => $description]
            ], 'Transaction already recorded (idempotent).');
        }

        $transactionId = 'tx_' . bin2hex(random_bytes(10));

        try {
            $db->beginTransaction();

            $stmt = $db->prepare("INSERT INTO transactions (id, user_id, transaction_type, amount, description, category_id, category_name, transaction_date, transaction_time, transaction_timestamp, notes, idempotency_key, client_local_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            $stmt->execute([
                $transactionId,
                $userId,
                $type,
                $amount,
                $description,
                $categoryId ?: null,
                $categoryName,
                $transactionDate,
                $transactionTime,
                $timestamp,
                $notes ?: null,
                $idempotencyKey,
                $clientLocalId ?: null
            ]);

            // Update primary account balance
            $balanceDelta = ($type === 'INCOME') ? $amount : -$amount;
            $accUpd = $db->prepare("UPDATE accounts SET current_balance = current_balance + ? WHERE user_id = ? AND is_primary = 1");
            $accUpd->execute([$balanceDelta, $userId]);

            $db->commit();
        } catch (Exception $e) {
            $db->rollBack();
            Response::error('Failed to save transaction: ' . $e->getMessage(), 500);
        }

        Response::success([
            'transaction' => [
                'id'              => $transactionId,
                'amount'          => $amount,
                'type'            => $type,
                'description'     => $description,
                'categoryName'    => $categoryName,
                'transactionDate' => $transactionDate,
                'transactionTime' => $transactionTime
            ]
        ], 'Transaction saved successfully in cPanel MySQL database.');
    }

    // PUT /api/transactions/{id}
    public static function update(string $id): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $body = Validator::getJsonBody();

        $db = Database::getConnection();
        $find = $db->prepare("SELECT id, amount, transaction_type FROM transactions WHERE id = ? AND user_id = ? LIMIT 1");
        $find->execute([$id, $userId]);
        $existing = $find->fetch();

        if (!$existing) {
            Response::notFound('Transaction not found');
        }

        $oldAmount = floatval($existing['amount']);
        $oldType = $existing['transaction_type'];

        $newAmount = isset($body['amount']) ? floatval($body['amount']) : $oldAmount;
        $newType = isset($body['type']) ? strtoupper(trim($body['type'])) : $oldType;
        $description = Validator::sanitizeString($body['description'] ?? null);
        $categoryName = Validator::sanitizeString($body['categoryName'] ?? null);
        $notes = Validator::sanitizeString($body['notes'] ?? null);

        try {
            $db->beginTransaction();

            $stmt = $db->prepare("UPDATE transactions SET amount = ?, transaction_type = ?, description = COALESCE(?, description), category_name = COALESCE(?, category_name), notes = COALESCE(?, notes), updated_at = NOW() WHERE id = ? AND user_id = ?");
            $stmt->execute([$newAmount, $newType, $description, $categoryName, $notes, $id, $userId]);

            // Adjust account balance difference
            $oldImpact = ($oldType === 'INCOME') ? $oldAmount : -$oldAmount;
            $newImpact = ($newType === 'INCOME') ? $newAmount : -$newAmount;
            $diff = $newImpact - $oldImpact;

            if ($diff != 0) {
                $accUpd = $db->prepare("UPDATE accounts SET current_balance = current_balance + ? WHERE user_id = ? AND is_primary = 1");
                $accUpd->execute([$diff, $userId]);
            }

            $db->commit();
        } catch (Exception $e) {
            $db->rollBack();
            Response::error('Failed to update transaction: ' . $e->getMessage(), 500);
        }

        Response::success(null, 'Transaction updated successfully.');
    }

    // DELETE /api/transactions/{id}
    public static function delete(string $id): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $db = Database::getConnection();

        $find = $db->prepare("SELECT id, amount, transaction_type FROM transactions WHERE id = ? AND user_id = ? LIMIT 1");
        $find->execute([$id, $userId]);
        $existing = $find->fetch();

        if (!$existing) {
            Response::notFound('Transaction not found');
        }

        $amount = floatval($existing['amount']);
        $type = $existing['transaction_type'];

        try {
            $db->beginTransaction();

            $del = $db->prepare("DELETE FROM transactions WHERE id = ? AND user_id = ?");
            $del->execute([$id, $userId]);

            // Revert balance
            $revertDelta = ($type === 'INCOME') ? -$amount : $amount;
            $accUpd = $db->prepare("UPDATE accounts SET current_balance = current_balance + ? WHERE user_id = ? AND is_primary = 1");
            $accUpd->execute([$revertDelta, $userId]);

            $db->commit();
        } catch (Exception $e) {
            $db->rollBack();
            Response::error('Failed to delete transaction: ' . $e->getMessage(), 500);
        }

        Response::success(null, 'Transaction deleted successfully.');
    }

    // POST /api/transactions/sync-batch
    public static function syncBatch(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $body = Validator::getJsonBody();

        $pending = $body['pendingTransactions'] ?? [];
        if (!is_array($pending) || empty($pending)) {
            Response::success(['results' => []], 'No pending transactions to sync.');
        }

        $db = Database::getConnection();
        $results = [];

        foreach ($pending as $item) {
            $clientLocalId = $item['clientLocalId'] ?? $item['id'] ?? null;
            $idempotencyKey = $item['idempotencyKey'] ?? null;
            $amount = floatval($item['amount'] ?? 0);
            $type = strtoupper(trim($item['type'] ?? 'EXPENSE'));
            $description = Validator::sanitizeString($item['description'] ?? '');
            $categoryName = Validator::sanitizeString($item['categoryName'] ?? 'General');
            $date = trim($item['transactionDate'] ?? date('Y-m-d'));
            $time = trim($item['transactionTime'] ?? date('H:i:s'));
            $timestamp = intval($item['timestamp'] ?? (time() * 1000));
            $notes = Validator::sanitizeString($item['notes'] ?? null);

            if ($amount <= 0 || empty($description)) {
                $results[] = [
                    'clientLocalId' => $clientLocalId,
                    'serverId'      => null,
                    'status'        => 'REJECTED_INVALID_DATA'
                ];
                continue;
            }

            // Check if already synced by idempotency key
            if (!empty($idempotencyKey)) {
                $check = $db->prepare("SELECT id FROM transactions WHERE idempotency_key = ? AND user_id = ? LIMIT 1");
                $check->execute([$idempotencyKey, $userId]);
                $existing = $check->fetch();
                if ($existing) {
                    $results[] = [
                        'clientLocalId' => $clientLocalId,
                        'serverId'      => $existing['id'],
                        'status'        => 'ALREADY_SYNCED'
                    ];
                    continue;
                }
            }

            $serverId = 'tx_' . bin2hex(random_bytes(10));
            try {
                $stmt = $db->prepare("INSERT INTO transactions (id, user_id, transaction_type, amount, description, category_name, transaction_date, transaction_time, transaction_timestamp, notes, idempotency_key, client_local_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                $stmt->execute([
                    $serverId,
                    $userId,
                    $type,
                    $amount,
                    $description,
                    $categoryName,
                    $date,
                    $time,
                    $timestamp,
                    $notes ?: null,
                    $idempotencyKey ?: ('idem_' . $serverId),
                    $clientLocalId
                ]);

                // Update primary account balance
                $delta = ($type === 'INCOME') ? $amount : -$amount;
                $accUpd = $db->prepare("UPDATE accounts SET current_balance = current_balance + ? WHERE user_id = ? AND is_primary = 1");
                $accUpd->execute([$delta, $userId]);

                $results[] = [
                    'clientLocalId' => $clientLocalId,
                    'serverId'      => $serverId,
                    'status'        => 'SYNCED'
                ];
            } catch (Exception $e) {
                $results[] = [
                    'clientLocalId' => $clientLocalId,
                    'serverId'      => null,
                    'status'        => 'ERROR: ' . $e->getMessage()
                ];
            }
        }

        Response::success([
            'results' => $results
        ], 'Batch synchronization processed.');
    }

    // GET /api/transactions/all OR /api/user/sync-all
    // Fulfills Requirement 5: Complete data recovery after reinstall
    public static function pullAll(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $db = Database::getConnection();

        // 1. Fetch all transactions
        $txStmt = $db->prepare("SELECT id, amount, transaction_type as type, description, category_name as categoryName, category_id as categoryId, transaction_date as transactionDate, transaction_time as transactionTime, transaction_timestamp as timestamp, notes, idempotency_key as idempotencyKey, client_local_id as clientLocalId FROM transactions WHERE user_id = ? ORDER BY transaction_date DESC, transaction_time DESC");
        $txStmt->execute([$userId]);
        $transactions = $txStmt->fetchAll();

        foreach ($transactions as &$t) {
            $t['amount'] = floatval($t['amount']);
            $t['timestamp'] = intval($t['timestamp']);
        }

        // 2. Fetch categories
        $catStmt = $db->prepare("SELECT id, category_name as name, category_type as type, icon_name as iconName, color_hex as colorHex, is_default as isDefault FROM categories WHERE user_id = ? OR is_default = 1");
        $catStmt->execute([$userId]);
        $categories = $catStmt->fetchAll();

        foreach ($categories as &$c) {
            $c['isDefault'] = (bool)$c['isDefault'];
        }

        // 3. Fetch user settings
        $setStmt = $db->prepare("SELECT daily_spending_limit as dailySpendingLimit, selected_bank as selectedBank, currency, currency_symbol as currencySymbol, theme as themeMode, ai_provider as aiProvider, onboarding_completed as onboardingCompleted FROM user_settings WHERE user_id = ? LIMIT 1");
        $setStmt->execute([$userId]);
        $settings = $setStmt->fetch() ?: [];

        if (!empty($settings)) {
            $settings['dailySpendingLimit'] = floatval($settings['dailySpendingLimit']);
            $settings['onboardingCompleted'] = (bool)$settings['onboardingCompleted'];
        }

        // 4. Fetch primary account balance
        $accStmt = $db->prepare("SELECT opening_balance as initialBalance, current_balance as currentBalance, bank_name as bankName FROM accounts WHERE user_id = ? AND is_primary = 1 LIMIT 1");
        $accStmt->execute([$userId]);
        $account = $accStmt->fetch() ?: ['initialBalance' => 0.0, 'currentBalance' => 0.0, 'bankName' => 'State Bank of India (SBI)'];

        $account['initialBalance'] = floatval($account['initialBalance']);
        $account['currentBalance'] = floatval($account['currentBalance']);

        // 5. Fetch WhatsApp Integration state
        $waStmt = $db->prepare("SELECT connection_status, phone_number FROM integrations WHERE user_id = ? AND integration_type = 'WHATSAPP_BUSINESS_CLOUD' LIMIT 1");
        $waStmt->execute([$userId]);
        $wa = $waStmt->fetch();

        Response::success([
            'transactions' => $transactions,
            'categories'   => $categories,
            'settings'     => $settings,
            'account'      => $account,
            'whatsApp'     => [
                'connected'   => ($wa && $wa['connection_status'] === 'CONNECTED'),
                'phoneNumber' => $wa['phone_number'] ?? null,
                'status'      => $wa['connection_status'] ?? 'NOT_CONNECTED'
            ]
        ], 'Full cloud user dataset retrieved successfully.');
    }
}
