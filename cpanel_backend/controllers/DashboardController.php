<?php
/**
 * Dashboard & Financial Governance Aggregations Controller for cPanel
 */

require_once __DIR__ . '/../config/db.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../middleware/AuthMiddleware.php';

class DashboardController {

    // GET /api/dashboard/summary
    public static function summary(): void {
        $auth = AuthMiddleware::authenticate();
        $userId = $auth['user']['id'];
        $db = Database::getConnection();

        $today = date('Y-m-d');
        $firstDayOfMonth = date('Y-m-01');

        // 1. Fetch User Settings & Daily Limit
        $setStmt = $db->prepare("SELECT daily_spending_limit, selected_bank FROM user_settings WHERE user_id = ? LIMIT 1");
        $setStmt->execute([$userId]);
        $settings = $setStmt->fetch() ?: ['daily_spending_limit' => 500.00, 'selected_bank' => 'State Bank of India (SBI)'];
        $dailyLimit = floatval($settings['daily_spending_limit']);
        $bankName = $settings['selected_bank'];

        // 2. Fetch Primary Account Balance
        $accStmt = $db->prepare("SELECT opening_balance, current_balance, bank_name FROM accounts WHERE user_id = ? AND is_primary = 1 LIMIT 1");
        $accStmt->execute([$userId]);
        $account = $accStmt->fetch();

        $initialBalance = $account ? floatval($account['opening_balance']) : 0.0;
        $currentBalance = $account ? floatval($account['current_balance']) : 0.0;
        if (!empty($account['bank_name'])) $bankName = $account['bank_name'];

        // 3. Lifetime Totals
        $totStmt = $db->prepare("SELECT 
            COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END), 0) as totalIncome,
            COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END), 0) as totalExpenses
            FROM transactions WHERE user_id = ?");
        $totStmt->execute([$userId]);
        $totals = $totStmt->fetch();
        $totalIncome = floatval($totals['totalIncome']);
        $totalExpenses = floatval($totals['totalExpenses']);

        // 4. Today Aggregations
        $todayStmt = $db->prepare("SELECT 
            COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END), 0) as todayIncome,
            COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END), 0) as todayExpense
            FROM transactions WHERE user_id = ? AND transaction_date = ?");
        $todayStmt->execute([$userId, $today]);
        $todayAgg = $todayStmt->fetch();
        $todayIncome = floatval($todayAgg['todayIncome']);
        $todayExpense = floatval($todayAgg['todayExpense']);

        $remainingAllowance = max(0.0, $dailyLimit - $todayExpense);
        $utilizationPercentage = ($dailyLimit > 0) ? min(100.0, ($todayExpense / $dailyLimit) * 100.0) : 0.0;
        $isExceeded = ($todayExpense > $dailyLimit);

        // 5. Monthly Aggregations
        $monthStmt = $db->prepare("SELECT 
            COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END), 0) as monthIncome,
            COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END), 0) as monthExpense
            FROM transactions WHERE user_id = ? AND transaction_date >= ?");
        $monthStmt->execute([$userId, $firstDayOfMonth]);
        $monthAgg = $monthStmt->fetch();
        $monthIncome = floatval($monthAgg['monthIncome']);
        $monthExpense = floatval($monthAgg['monthExpense']);
        $monthlySavings = max(0.0, $monthIncome - $monthExpense);
        $spendingPercentage = ($monthIncome > 0) ? min(100.0, ($monthExpense / $monthIncome) * 100.0) : 0.0;

        // 6. Recent 5 Transactions
        $recStmt = $db->prepare("SELECT id, amount, transaction_type as type, description, category_name as categoryName, transaction_date as transactionDate, transaction_time as transactionTime, transaction_timestamp as timestamp FROM transactions WHERE user_id = ? ORDER BY transaction_date DESC, transaction_time DESC LIMIT 5");
        $recStmt->execute([$userId]);
        $recent = $recStmt->fetchAll();

        foreach ($recent as &$r) {
            $r['amount'] = floatval($r['amount']);
            $r['timestamp'] = intval($r['timestamp']);
        }

        Response::success([
            'balance' => [
                'currentBalance' => $currentBalance,
                'initialBalance' => $initialBalance,
                'totalIncome'    => $totalIncome,
                'totalExpenses'  => $totalExpenses,
                'bankName'       => $bankName
            ],
            'today' => [
                'income'                => $todayIncome,
                'expense'               => $todayExpense,
                'dailyLimit'            => $dailyLimit,
                'remainingAllowance'    => $remainingAllowance,
                'utilizationPercentage' => round($utilizationPercentage, 1),
                'isExceeded'            => $isExceeded
            ],
            'monthly' => [
                'income'             => $monthIncome,
                'expense'            => $monthExpense,
                'savings'            => $monthlySavings,
                'spendingPercentage' => round($spendingPercentage, 1)
            ],
            'recentTransactions' => $recent
        ]);
    }
}
