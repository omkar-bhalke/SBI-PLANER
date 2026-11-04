<?php
/**
 * FinPulse Central REST API Front Controller & Router for cPanel
 * Target Domain: https://radhewins.buzz
 */

require_once __DIR__ . '/config/cors.php';
require_once __DIR__ . '/config/db.php';
require_once __DIR__ . '/helpers/Response.php';

// Load Controllers
require_once __DIR__ . '/controllers/AuthController.php';
require_once __DIR__ . '/controllers/TransactionController.php';
require_once __DIR__ . '/controllers/DashboardController.php';
require_once __DIR__ . '/controllers/SettingsController.php';
require_once __DIR__ . '/controllers/AiController.php';
require_once __DIR__ . '/controllers/IntegrationController.php';
require_once __DIR__ . '/controllers/RestrictionController.php';

// 1. Resolve Route & Method
$method = $_SERVER['REQUEST_METHOD'];

// Extract route from query parameter (set by .htaccess) or fallback to REQUEST_URI
$route = $_GET['route'] ?? '';
if (empty($route)) {
    $uri = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
    $scriptDir = dirname($_SERVER['SCRIPT_NAME']);
    if ($scriptDir !== '/' && strpos($uri, $scriptDir) === 0) {
        $uri = substr($uri, strlen($scriptDir));
    }
    $route = trim($uri, '/');
}

// Normalize: remove leading 'api/' if present
if (strpos($route, 'api/') === 0) {
    $route = substr($route, 4);
}
$route = trim($route, '/');

// 2. Health Check & Root Ping
if ($route === '' || $route === 'health' || $route === 'api') {
    Response::success([
        'service'    => 'FinPulse cPanel REST API',
        'domain'     => 'https://radhewins.buzz',
        'status'     => 'OPERATIONAL',
        'version'    => '2.1.0',
        'serverTime' => date('Y-m-d H:i:s T')
    ], 'FinPulse backend is online and connected to cPanel MySQL.');
}

// 3. REST Routing Table
try {
    // ------------------------------------------
    // AUTHENTICATION & DEVICE BINDING (TASKS 1 & 2)
    // ------------------------------------------
    if ($route === 'auth/register' && $method === 'POST') {
        AuthController::register();
    } elseif ($route === 'auth/login' && $method === 'POST') {
        AuthController::login();
    } elseif ($route === 'auth/forgot-password' && $method === 'POST') {
        AuthController::forgotPassword();
    } elseif ($route === 'auth/reset-password' && $method === 'POST') {
        AuthController::resetPassword();
    } elseif ($route === 'auth/refresh-token' && $method === 'POST') {
        AuthController::refreshToken();
    } elseif ($route === 'auth/device-status' && $method === 'GET') {
        AuthController::deviceStatus();
    } elseif ($route === 'auth/device-reset' && $method === 'POST') {
        AuthController::deviceReset();
    }

    // ------------------------------------------
    // DASHBOARD & SUMMARY
    // ------------------------------------------
    elseif ($route === 'dashboard/summary' && $method === 'GET') {
        DashboardController::summary();
    }

    // ------------------------------------------
    // TRANSACTIONS & CLOUD RESTORE (TASKS 4 & 5)
    // ------------------------------------------
    elseif ($route === 'transactions' && $method === 'GET') {
        TransactionController::list();
    } elseif ($route === 'transactions' && $method === 'POST') {
        TransactionController::create();
    } elseif ($route === 'transactions/sync-batch' && $method === 'POST') {
        TransactionController::syncBatch();
    } elseif (($route === 'transactions/all' || $route === 'user/sync-all') && $method === 'GET') {
        TransactionController::pullAll();
    } elseif (preg_match('#^transactions/([^/]+)$#', $route, $matches)) {
        $transactionId = $matches[1];
        if ($method === 'PUT') {
            TransactionController::update($transactionId);
        } elseif ($method === 'DELETE') {
            TransactionController::delete($transactionId);
        }
    }

    // ------------------------------------------
    // FINANCIAL SETTINGS & CATEGORIES
    // ------------------------------------------
    elseif ($route === 'settings' && $method === 'GET') {
        SettingsController::get();
    } elseif ($route === 'settings/daily-limit' && $method === 'PUT') {
        SettingsController::updateDailyLimit();
    } elseif ($route === 'settings/initial-balance' && $method === 'PUT') {
        SettingsController::updateInitialBalance();
    } elseif ($route === 'settings/bank' && $method === 'PUT') {
        SettingsController::updateBank();
    } elseif ($route === 'categories' && $method === 'GET') {
        SettingsController::listCategories();
    } elseif ($route === 'categories' && $method === 'POST') {
        SettingsController::addCategory();
    } elseif (preg_match('#^categories/([^/]+)$#', $route, $matches) && $method === 'DELETE') {
        SettingsController::deleteCategory($matches[1]);
    }

    // ------------------------------------------
    // REAL AI CONFIGURATION & ANALYSIS (TASK 4)
    // ------------------------------------------
    elseif ($route === 'ai/config' && $method === 'GET') {
        AiController::getConfig();
    } elseif ($route === 'ai/validate-and-save' && $method === 'POST') {
        AiController::validateAndSave();
    } elseif ($route === 'ai/analyze' && $method === 'POST') {
        AiController::analyze();
    } elseif ($route === 'ai/emergency-review' && $method === 'POST') {
        AiController::emergencyReview();
    }

    // ------------------------------------------
    // REAL META WHATSAPP CLOUD API (TASK 3)
    // ------------------------------------------
    elseif ($route === 'integrations/whatsapp/status' && $method === 'GET') {
        IntegrationController::whatsappStatus();
    } elseif ($route === 'integrations/whatsapp/connect' && $method === 'POST') {
        IntegrationController::whatsappConnect();
    } elseif ($route === 'integrations/whatsapp/disconnect' && $method === 'DELETE') {
        IntegrationController::whatsappDisconnect();
    }

    // ------------------------------------------
    // REAL APP RESTRICTIONS & ACCESSIBILITY (TASK 5)
    // ------------------------------------------
    elseif ($route === 'restrictions' && $method === 'GET') {
        RestrictionController::get();
    } elseif ($route === 'restrictions' && $method === 'POST') {
        RestrictionController::update();
    } elseif ($route === 'restrictions/log-event' && $method === 'POST') {
        RestrictionController::logEvent();
    }

    // ------------------------------------------
    // 404 NOT FOUND
    // ------------------------------------------
    else {
        Response::notFound("Endpoint not found: [$method] /$route");
    }

} catch (Throwable $e) {
    Response::error("Internal Server Error: " . $e->getMessage(), 500);
}
