-- =====================================================================
-- FinPulse - Complete cPanel MySQL Database Schema
-- Target Domain: https://radhewins.buzz
-- Compatible with phpMyAdmin, MySQL 5.7+, MySQL 8.0+, MariaDB 10.3+
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

-- --------------------------------------------------------
-- Table: users (Email/Password Auth & Profile)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_email_verified` tinyint(1) NOT NULL DEFAULT 1,
  `verification_otp` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `otp_expires_at` datetime DEFAULT NULL,
  `reset_password_otp` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reset_otp_expires_at` datetime DEFAULT NULL,
  `profile_image` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: device_bindings (One Device = One Account Enforcement)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `device_bindings`;
CREATE TABLE `device_bindings` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `device_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `device_fingerprint` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `device_model` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `os_version` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `app_version` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT '2.1.0',
  `status` enum('ACTIVE','SUSPENDED','RELEASED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `last_seen_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  KEY `idx_device_user` (`user_id`),
  CONSTRAINT `fk_device_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: user_settings (Daily Limits, Selected Bank, Theme)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `user_settings`;
CREATE TABLE `user_settings` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `currency` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'INR',
  `currency_symbol` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '₹',
  `theme` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYSTEM',
  `onboarding_completed` tinyint(1) NOT NULL DEFAULT 0,
  `daily_spending_limit` decimal(12,2) NOT NULL DEFAULT 500.00,
  `selected_bank` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'State Bank of India (SBI)',
  `ai_provider` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'GEMINI',
  `accessibility_enabled` tinyint(1) NOT NULL DEFAULT 0,
  `notifications_enabled` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_settings` (`user_id`),
  CONSTRAINT `fk_settings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: accounts (Primary Bank / Cash Accounts & Balances)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `accounts`;
CREATE TABLE `accounts` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `bank_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'State Bank of India (SBI)',
  `account_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SAVINGS',
  `opening_balance` decimal(14,2) NOT NULL DEFAULT 0.00,
  `current_balance` decimal(14,2) NOT NULL DEFAULT 0.00,
  `is_primary` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_accounts_user` (`user_id`),
  CONSTRAINT `fk_accounts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: categories (Income & Expense Categories)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `category_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_type` enum('INCOME','EXPENSE') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EXPENSE',
  `icon_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'category',
  `color_hex` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#1E3E62',
  `is_default` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_categories_user` (`user_id`,`category_type`),
  CONSTRAINT `fk_categories_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert Default System Categories
INSERT INTO `categories` (`id`, `user_id`, `category_name`, `category_type`, `icon_name`, `color_hex`, `is_default`) VALUES
('cat_default_food', NULL, 'Food & Dining', 'EXPENSE', 'restaurant', '#FF6B6B', 1),
('cat_default_groceries', NULL, 'Groceries', 'EXPENSE', 'shopping_cart', '#4ECDC4', 1),
('cat_default_transport', NULL, 'Transportation', 'EXPENSE', 'directions_bus', '#45B7D1', 1),
('cat_default_bills', NULL, 'Utilities & Bills', 'EXPENSE', 'receipt_long', '#F7B731', 1),
('cat_default_entertainment', NULL, 'Entertainment', 'EXPENSE', 'movie', '#A55EEA', 1),
('cat_default_health', NULL, 'Healthcare', 'EXPENSE', 'local_hospital', '#FC5C65', 1),
('cat_default_education', NULL, 'Education & Books', 'EXPENSE', 'school', '#26DE81', 1),
('cat_default_salary', NULL, 'Salary & Allowance', 'INCOME', 'account_balance_wallet', '#2ED573', 1),
('cat_default_invest', NULL, 'Investments & Gifts', 'INCOME', 'savings', '#1E90FF', 1);

-- --------------------------------------------------------
-- Table: transactions (Expenses and Incomes)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `transactions`;
CREATE TABLE `transactions` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `transaction_type` enum('INCOME','EXPENSE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `category_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `transaction_date` date NOT NULL,
  `transaction_time` time NOT NULL,
  `transaction_timestamp` bigint(20) NOT NULL,
  `notes` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `idempotency_key` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `client_local_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotency_key` (`idempotency_key`),
  KEY `idx_trans_user_date` (`user_id`,`transaction_date`),
  KEY `idx_trans_user_type` (`user_id`,`transaction_type`),
  KEY `idx_trans_user_cat` (`user_id`,`category_name`),
  CONSTRAINT `fk_transactions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: user_ai_configs (Gemini / OpenAI Keys Encrypted at Rest)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `user_ai_configs`;
CREATE TABLE `user_ai_configs` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider` enum('GEMINI','OPENAI') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'GEMINI',
  `encrypted_api_key` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `key_status` enum('VALID','INVALID','UNCHECKED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'VALID',
  `last_validated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_ai_provider` (`user_id`,`provider`),
  CONSTRAINT `fk_ai_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: app_restrictions (Selected Payment Apps to Monitor)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `app_restrictions`;
CREATE TABLE `app_restrictions` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `package_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `app_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_enabled` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_pkg` (`user_id`,`package_name`),
  CONSTRAINT `fk_restriction_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: app_restriction_events (Logged Overspending Interceptions)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `app_restriction_events`;
CREATE TABLE `app_restriction_events` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `package_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `app_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `daily_limit` decimal(12,2) NOT NULL,
  `amount_spent` decimal(12,2) NOT NULL,
  `amount_exceeded` decimal(12,2) NOT NULL,
  `action_taken` enum('BLOCKED','EMERGENCY_OVERRIDE','DISMISSED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'BLOCKED',
  `event_timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_events_user_time` (`user_id`,`event_timestamp`),
  CONSTRAINT `fk_events_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: emergency_requests (Emergency Allowance Reviews)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `emergency_requests`;
CREATE TABLE `emergency_requests` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `requested_amount` decimal(12,2) NOT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `screenshot_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ai_recommendation` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ai_confidence` decimal(4,2) DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED','EXPIRED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `expires_at` datetime DEFAULT NULL,
  `confirmed_by_user` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_emergency_user` (`user_id`,`status`),
  CONSTRAINT `fk_emergency_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: integrations (WhatsApp Business Cloud API)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `integrations`;
CREATE TABLE `integrations` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `integration_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'WHATSAPP_BUSINESS_CLOUD',
  `connection_status` enum('NOT_CONNECTED','CONNECTING','CONNECTED','CONNECTION_FAILED','DISCONNECTED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NOT_CONNECTED',
  `phone_number` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `configuration_reference` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_synced_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_integration` (`user_id`,`integration_type`),
  CONSTRAINT `fk_integrations_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: user_tokens (JWT Refresh Tokens and Session Revocation)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `user_tokens`;
CREATE TABLE `user_tokens` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `device_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `refresh_token` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tokens_refresh` (`refresh_token`),
  KEY `idx_tokens_user` (`user_id`),
  CONSTRAINT `fk_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: pending_transactions (SBI WhatsApp Mini Statement Ingestion)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `pending_transactions`;
CREATE TABLE `pending_transactions` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SBI_WHATSAPP_STATEMENT',
  `account_number` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'xxx188',
  `transaction_date` date NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `type` enum('DEBIT','CREDIT') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DEBIT',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `suggested_category` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Bank Transfer',
  `idempotency_hash` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('PENDING','CONFIRMED','DISMISSED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `confirmed_transaction_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pending_idempotency` (`user_id`, `idempotency_hash`),
  KEY `idx_pending_user_status` (`user_id`, `status`),
  CONSTRAINT `fk_pending_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: balance_history (Historical Bank Balances from Statements)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `balance_history`;
CREATE TABLE `balance_history` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_number` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'xxx188',
  `balance` decimal(14,2) NOT NULL,
  `raw_statement_date` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `statement_text` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SBI_WHATSAPP',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_balance_user_created` (`user_id`, `created_at`),
  CONSTRAINT `fk_balance_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: automation_settings (Config for Railway & SBI Automated Cycles)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `automation_settings`;
CREATE TABLE `automation_settings` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sbi_target_phone` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '919022690226',
  `interval_minutes` int(11) NOT NULL DEFAULT 30,
  `is_enabled` tinyint(1) NOT NULL DEFAULT 1,
  `railway_api_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `railway_api_token` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_run_at` datetime DEFAULT NULL,
  `last_status` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'IDLE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_automation_user` (`user_id`),
  CONSTRAINT `fk_automation_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: automation_logs (Audit Trail of Automation & Statement Fetches)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `automation_logs`;
CREATE TABLE `automation_logs` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `details` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_logs_user_created` (`user_id`, `created_at`),
  CONSTRAINT `fk_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
