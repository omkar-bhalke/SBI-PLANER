<?php
/**
 * Pending Transactions Controller for SBI Mini Statement Ingestion & User Review
 * Target Domain: https://radhewins.buzz
 */

require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../middleware/AuthMiddleware.php';
require_once __DIR__ . '/../config/db.php';

class PendingTransactionController {

    /**
     * Railway Worker: Ingest newly parsed transactions and balance
     * POST /api/automation/sync-pending
     */
    public static function syncFromWorker(): void {
        $auth = AuthMiddleware::authenticate();
        $user = $auth['user'];
        $userId = $user['id'];

        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) {
            Response::error('Invalid JSON payload in request body', 400);
        }

        $accountNumber = trim($input['account_number'] ?? $input['accountNumber'] ?? 'xxx188');
        $balance = isset($input['balance']) ? (float)$input['balance'] : null;
        $statementText = $input['statement_text'] ?? $input['statementText'] ?? null;
        $statementDate = $input['statement_date'] ?? $input['statementDate'] ?? date('d/m/Y, H:i:s');
        $transactions = $input['transactions'] ?? [];

        $db = Database::getConnection();
        $db->beginTransaction();

        try {
            $insertedCount = 0;
            $skippedCount = 0;

            // 1. Process and deduplicate pending transactions
            if (!empty($transactions) && is_array($transactions)) {
                $checkStmt = $db->prepare("SELECT id FROM pending_transactions WHERE user_id = ? AND idempotency_hash = ? LIMIT 1");
                $insertStmt = $db->prepare("
                    INSERT INTO pending_transactions 
                    (id, user_id, source, account_number, transaction_date, amount, type, description, suggested_category, idempotency_hash, status, created_at)
                    VALUES (?, ?, 'SBI_WHATSAPP_STATEMENT', ?, ?, ?, ?, ?, ?, ?, 'PENDING', NOW())
                ");

                foreach ($transactions as $tx) {
                    $dateStr = $tx['date'] ?? date('Y-m-d');
                    // Format date if in DD/MM/YYYY
                    if (preg_match('/^(\d{2})\/(\d{2})\/(\d{4})$/', $dateStr, $m)) {
                        $parsedDate = "{$m[3]}-{$m[2]}-{$m[1]}";
                    } else {
                        $parsedDate = date('Y-m-d', strtotime($dateStr) ?: time());
                    }

                    $amount = abs((float)($tx['amount'] ?? 0));
                    $type = strtoupper($tx['type'] ?? 'DEBIT');
                    if ($type !== 'CREDIT') $type = 'DEBIT';
                    $description = trim($tx['description'] ?? 'SBI Mini Statement Entry');
                    $suggestedCat = trim($tx['suggestedCategory'] ?? $tx['suggested_category'] ?? 'Bank Transfer');
                    $hash = trim($tx['idempotencyHash'] ?? $tx['idempotency_hash'] ?? hash('sha256', "{$userId}_{$accountNumber}_{$parsedDate}_{$amount}_{$type}_{$description}"));

                    $checkStmt->execute([$userId, $hash]);
                    if ($checkStmt->fetch()) {
                        $skippedCount++;
                        continue;
                    }

                    $pendingId = 'pt_' . bin2hex(random_bytes(16));
                    $insertStmt->execute([
                        $pendingId,
                        $userId,
                        $accountNumber,
                        $parsedDate,
                        $amount,
                        $type,
                        $description,
                        $suggestedCat,
                        $hash
                    ]);
                    $insertedCount++;
                }
            }

            // 2. Update balance history & account if balance provided
            if ($balance !== null) {
                $bhId = 'bh_' . bin2hex(random_bytes(16));
                $bhStmt = $db->prepare("
                    INSERT INTO balance_history (id, user_id, account_number, balance, raw_statement_date, statement_text, source, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, 'SBI_WHATSAPP', NOW())
                ");
                $bhStmt->execute([$bhId, $userId, $accountNumber, $balance, $statementDate, $statementText]);

                // Update accounts table current balance
                $accStmt = $db->prepare("UPDATE accounts SET current_balance = ?, updated_at = NOW() WHERE user_id = ? AND is_primary = 1");
                $accStmt->execute([$balance, $userId]);
            }

            // 3. Update automation_settings last run
            $setStmt = $db->prepare("
                UPDATE automation_settings 
                SET last_run_at = NOW(), last_status = 'SUCCESS' 
                WHERE user_id = ?
            ");
            $setStmt->execute([$userId]);

            // 4. Log event
            $logId = 'log_' . bin2hex(random_bytes(16));
            $logStmt = $db->prepare("
                INSERT INTO automation_logs (id, user_id, event_type, message, details, created_at)
                VALUES (?, ?, 'STATEMENT_INGESTED', ?, ?, NOW())
            ");
            $logStmt->execute([
                $logId,
                $userId,
                "Ingested SBI statement: $insertedCount new pending transactions, $skippedCount duplicates. Balance: ₹$balance",
                json_encode(['inserted' => $insertedCount, 'skipped' => $skippedCount, 'balance' => $balance])
            ]);

            $db->commit();

            Response::success([
                'inserted'      => $insertedCount,
                'skipped'       => $skippedCount,
                'latestBalance' => $balance,
                'accountNumber' => $accountNumber
            ], "Statement processed successfully: $insertedCount new transactions queued for review.");

        } catch (Throwable $e) {
            $db->rollBack();
            Response::error('Failed to sync statement: ' . $e->getMessage(), 500);
        }
    }

    /**
     * Android App: List pending transactions awaiting user confirmation
     * GET /api/pending-transactions
     */
    public static function listPending(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];

        $db = Database::getConnection();
        $stmt = $db->prepare("
            SELECT 
                id, 
                source, 
                account_number AS accountNumber, 
                transaction_date AS transactionDate, 
                amount, 
                type, 
                description, 
                suggested_category AS suggestedCategory, 
                idempotency_hash AS idempotencyHash, 
                status, 
                created_at AS createdAt
            FROM pending_transactions
            WHERE user_id = ? AND status = 'PENDING'
            ORDER BY transaction_date DESC, created_at DESC
        ");
        $stmt->execute([$userId]);
        $items = $stmt->fetchAll();

        // Convert types
        foreach ($items as &$it) {
            $it['amount'] = (float)$it['amount'];
        }

        Response::success([
            'count' => count($items),
            'items' => $items
        ]);
    }

    /**
     * Android App: User confirms a pending transaction
     * POST /api/pending-transactions/confirm
     */
    public static function confirm(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];

        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) {
            Response::error('Invalid JSON payload', 400);
        }

        $pendingId = trim($input['pendingTransactionId'] ?? $input['id'] ?? '');
        $categoryName = trim($input['categoryName'] ?? 'General');
        $notes = trim($input['notes'] ?? $input['purpose'] ?? '');

        if (empty($pendingId)) {
            Response::error('pendingTransactionId is required', 422);
        }

        $db = Database::getConnection();
        $db->beginTransaction();

        try {
            // Find pending item
            $stmt = $db->prepare("SELECT * FROM pending_transactions WHERE id = ? AND user_id = ? LIMIT 1");
            $stmt->execute([$pendingId, $userId]);
            $pending = $stmt->fetch();

            if (!$pending) {
                $db->rollBack();
                Response::notFound('Pending transaction not found');
            }

            if ($pending['status'] !== 'PENDING') {
                $db->rollBack();
                Response::error("Transaction is already {$pending['status']}", 400);
            }

            // Create permanent transaction record in transactions table
            $transId = 'tx_' . bin2hex(random_bytes(16));
            $txType = ($pending['type'] === 'CREDIT') ? 'INCOME' : 'EXPENSE';
            $transDate = $pending['transaction_date'];
            $transTime = date('H:i:s');
            $timestamp = strtotime("$transDate $transTime") ?: time();

            $insertTx = $db->prepare("
                INSERT INTO transactions 
                (id, user_id, transaction_type, amount, description, category_name, transaction_date, transaction_time, transaction_timestamp, notes, idempotency_key, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            ");
            $insertTx->execute([
                $transId,
                $userId,
                $txType,
                $pending['amount'],
                $pending['description'],
                $categoryName,
                $transDate,
                $transTime,
                $timestamp,
                $notes,
                'confirmed_' . $pending['idempotency_hash']
            ]);

            // Mark pending item as CONFIRMED
            $updatePending = $db->prepare("
                UPDATE pending_transactions 
                SET status = 'CONFIRMED', confirmed_transaction_id = ?, updated_at = NOW() 
                WHERE id = ?
            ");
            $updatePending->execute([$transId, $pendingId]);

            $db->commit();

            Response::success([
                'transactionId' => $transId,
                'status'        => 'CONFIRMED',
                'amount'        => (float)$pending['amount'],
                'category'      => $categoryName,
                'description'   => $pending['description']
            ], 'Transaction successfully confirmed and added to your ledger.');

        } catch (Throwable $e) {
            $db->rollBack();
            Response::error('Failed to confirm transaction: ' . $e->getMessage(), 500);
        }
    }

    /**
     * Android App: User dismisses a pending transaction
     * POST /api/pending-transactions/dismiss
     */
    public static function dismiss(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];

        $input = json_decode(file_get_contents('php://input'), true);
        $pendingId = trim($input['pendingTransactionId'] ?? $input['id'] ?? '');

        if (empty($pendingId)) {
            Response::error('pendingTransactionId is required', 422);
        }

        $db = Database::getConnection();
        $stmt = $db->prepare("
            UPDATE pending_transactions 
            SET status = 'DISMISSED', updated_at = NOW() 
            WHERE id = ? AND user_id = ?
        ");
        $stmt->execute([$pendingId, $userId]);

        Response::success(['status' => 'DISMISSED'], 'Transaction dismissed.');
    }
}
