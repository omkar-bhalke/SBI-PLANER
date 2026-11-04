-- =====================================================================
-- FinPulse - Personal Expense Management & AI Financial Assistant
-- Database Schema for MySQL 8.0+ / cPanel MySQL Database
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Users Table
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `google_id` VARCHAR(128) UNIQUE NOT NULL,
    `email` VARCHAR(255) UNIQUE NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `profile_image` VARCHAR(512) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_users_email` (`email`),
    INDEX `idx_users_google_id` (`google_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. User Settings Table
DROP TABLE IF EXISTS `user_settings`;
CREATE TABLE `user_settings` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(64) NOT NULL,
    `currency` VARCHAR(8) DEFAULT 'INR',
    `currency_symbol` VARCHAR(8) DEFAULT '₹',
    `theme` VARCHAR(16) DEFAULT 'SYSTEM',
    `onboarding_completed` BOOLEAN DEFAULT FALSE,
    `daily_spending_limit` DECIMAL(12, 2) DEFAULT 500.00,
    `selected_bank` VARCHAR(64) DEFAULT 'State Bank of India (SBI)',
    `ai_provider` VARCHAR(32) DEFAULT 'GEMINI',
    `notifications_enabled` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_settings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_settings` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Accounts Table
DROP TABLE IF EXISTS `accounts`;
CREATE TABLE `accounts` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(64) NOT NULL,
    `bank_name` VARCHAR(128) NOT NULL DEFAULT 'State Bank of India (SBI)',
    `account_type` VARCHAR(32) DEFAULT 'SAVINGS',
    `opening_balance` DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    `current_balance` DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    `is_primary` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_accounts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_accounts_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Categories Table
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(64) NULL, -- NULL means system default category
    `category_name` VARCHAR(64) NOT NULL,
    `category_type` ENUM('INCOME', 'EXPENSE') NOT NULL DEFAULT 'EXPENSE',
    `icon_name` VARCHAR(64) DEFAULT 'category',
    `color_hex` VARCHAR(16) DEFAULT '#1E3E62',
    `is_default` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_categories_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_categories_user` (`user_id`, `category_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Transactions Table
DROP TABLE IF EXISTS `transactions`;
CREATE TABLE `transactions` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(64) NOT NULL,
    `account_id` VARCHAR(64) NULL,
    `transaction_type` ENUM('INCOME', 'EXPENSE') NOT NULL,
    `amount` DECIMAL(14, 2) NOT NULL,
    `description` VARCHAR(255) NOT NULL,
    `category_id` VARCHAR(64) NULL,
    `category_name` VARCHAR(64) NOT NULL,
    `transaction_date` DATE NOT NULL,
    `transaction_time` TIME NOT NULL,
    `transaction_timestamp` BIGINT NOT NULL,
    `notes` TEXT DEFAULT NULL,
    `idempotency_key` VARCHAR(128) UNIQUE DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_transactions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_transactions_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE SET NULL,
    INDEX `idx_trans_user_date` (`user_id`, `transaction_date`),
    INDEX `idx_trans_user_type` (`user_id`, `transaction_type`),
    INDEX `idx_trans_user_category` (`user_id`, `category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Emergency Requests Table (Future Approval Module)
DROP TABLE IF EXISTS `emergency_requests`;
CREATE TABLE `emergency_requests` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(64) NOT NULL,
    `requested_amount` DECIMAL(12, 2) NOT NULL,
    `reason` TEXT NOT NULL,
    `screenshot_url` VARCHAR(512) DEFAULT NULL,
    `ai_recommendation` TEXT DEFAULT NULL,
    `ai_confidence` DECIMAL(4, 2) DEFAULT NULL,
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED') DEFAULT 'PENDING',
    `expires_at` TIMESTAMP NULL,
    `confirmed_by_user` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_emergency_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_emergency_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Integrations Table (WhatsApp, Bank, Future Extension)
DROP TABLE IF EXISTS `integrations`;
CREATE TABLE `integrations` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(64) NOT NULL,
    `integration_type` VARCHAR(64) NOT NULL, -- 'WHATSAPP', 'SBI_BANK', 'PAYMENT_CONTROL'
    `connection_status` ENUM('CONNECTED', 'DISCONNECTED', 'PENDING', 'DISABLED') DEFAULT 'DISCONNECTED',
    `phone_number` VARCHAR(32) DEFAULT NULL,
    `configuration_reference` TEXT DEFAULT NULL,
    `last_synced_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_integrations_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_integration` (`user_id`, `integration_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
