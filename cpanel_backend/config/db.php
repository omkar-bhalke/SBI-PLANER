<?php
/**
 * FinPulse Backend - Database Connection Configuration for cPanel
 * Domain: https://radhewins.buzz
 *
 * INSTRUCTIONS FOR CPANEL:
 * 1. Create a MySQL database in cPanel -> MySQL Databases (e.g. u123456_finpulse).
 * 2. Create a MySQL user and password, then assign the user to the database with ALL PRIVILEGES.
 * 3. Update DB_NAME, DB_USER, and DB_PASS below with your cPanel database credentials.
 * 4. Keep DB_HOST as 'localhost' (standard for 99% of cPanel hosting).
 */

define('DB_HOST', 'localhost');
define('DB_NAME', 'radhewin_finpulse');       // Replace with your cPanel database name (e.g. username_dbname)
define('DB_USER', 'radhewin_dbuser');         // Replace with your cPanel database username
define('DB_PASS', 'ChangeMeStrongPass2026!'); // Replace with your cPanel database user password
define('DB_CHARSET', 'utf8mb4');

// Cryptographic secret key for signing JWT tokens and AES-256-CBC at rest encryption
define('JWT_SECRET_KEY', 'finpulse_cpanel_jwt_secret_radhewins_buzz_9823471029384756');
define('AES_ENCRYPTION_KEY', 'finpulse_aes256_enc_key_radhewins_buzz_2026');

class Database {
    private static ?PDO $instance = null;

    public static function getConnection(): PDO {
        if (self::$instance === null) {
            $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=" . DB_CHARSET;
            $options = [
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES   => false,
                PDO::MYSQL_ATTR_INIT_COMMAND => "SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci"
            ];

            try {
                self::$instance = new PDO($dsn, DB_USER, DB_PASS, $options);
            } catch (PDOException $e) {
                // Do not expose database credentials or internal stack trace to client
                http_response_code(500);
                header('Content-Type: application/json; charset=UTF-8');
                echo json_encode([
                    'success' => false,
                    'message' => 'Database connection failed. Please ensure cPanel database credentials in config/db.php are correct.',
                    'error'   => $e->getMessage()
                ]);
                exit;
            }
        }
        return self::$instance;
    }
}
