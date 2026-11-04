-- =====================================================================
-- FinPulse - Personal Expense Management & AI Financial Assistant
-- Database Schema for MySQL 8.0+ / cPanel MySQL Database
-- Upgraded: Email/Password Auth, One Device = One Account, Real AI,
-- and App Restriction Tracking
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Users Table (Email & Password Authentication)
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `email` VARCHAR(255) UNIQUE NOT NULL,
    `password_hash` VARCHAR(255) NULL,
    `name` VARCHAR(255) NOT NULL,
    `google_id` VARCHAR(128) NULL,
    `is_email_verified` BOOLEAN DEFAULT TRUE,
    `verification_otp` VARCHAR(16) NULL,
    `otp_expires_at` TIMESTAMP NULL,
    `reset_password_otp` VARCHAR(16) NULL,
    `reset_otp_expires_at` TIMESTAMP NULL,
    `profile_image` VARCHAR(512) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. One Device = One Account: Device Bindings Table
DROP TABLE IF EXISTS `device_bindings`;
CREATE TABLE `device_bindings` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `device_id` VARCHAR(128) UNIQUE NOT NULL, -- Enforces ONE ACCOUNT PER DEVICE strictly
    `user_id` VARCHAR(64) NOT NULL,
    `device_fingerprint` VARCHAR(255) NULL,
    `device_model` VARCHAR(128) NULL,
    `os_version` VARCHAR(32) NULL,
    `app_version` VARCHAR(32) DEFAULT '2.0.0',
    `status` ENUM('ACTIVE', 'SUSPENDED', 'RELEASED') DEFAULT 'ACTIVE',
    `last_seen_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_device_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_device_binding_user` (`user_id`),
    INDEX `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. User Settings Table
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
    `accessibility_enabled` BOOLEAN DEFAULT FALSE,
    `notifications_enabled` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_settings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_settings` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Accounts Table
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

-- 5. Categories Table
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(64) NULL,
    `category_name` VARCHAR(64) NOT NULL,
    `category_type` ENUM('INCOME', 'EXPENSE') NOT NULL DEFAULT 'EXPENSE',
    `icon_name` VARCHAR(64) DEFAULT 'category',
    `color_hex` VARCHAR(16) DEFAULT '#1E3E62',
    `is_default` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_categories_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_categories_user` (`user_id`, `category_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Transactions Table
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

-- 7. User Real AI Configurations Table (Encrypted at rest)
DROP TABLE IF EXISTS `user_ai_configs`;
CREATE TABLE `user_ai_configs` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(64) NOT NULL,
    `provider` ENUM('GEMINI', 'OPENAI') NOT NULL DEFAULT 'GEMINI',
    `encrypted_api_key` TEXT NOT NULL,
    `key_status` ENUM('VALID', 'INVALID', 'UNCHECKED') DEFAULT 'VALID',
    `last_validated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_ai_config_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_ai_provider` (`user_id`, `provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. App Restrictions Configuration Table
DROP TABLE IF EXISTS `app_restrictions`;
CREATE TABLE `app_restrictions` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(64) NOT NULL,
    `package_name` VARCHAR(255) NOT NULL,
    `app_name` VARCHAR(128) NOT NULL,
    `is_enabled` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_restriction_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_app` (`user_id`, `package_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. App Restriction Interception Events Table
DROP TABLE IF EXISTS `app_restriction_events`;
CREATE TABLE `app_restriction_events` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(64) NOT NULL,
    `package_name` VARCHAR(255) NOT NULL,
    `app_name` VARCHAR(128) NOT NULL,
    `daily_limit` DECIMAL(12, 2) NOT NULL,
    `amount_spent` DECIMAL(12, 2) NOT NULL,
    `amount_exceeded` DECIMAL(12, 2) NOT NULL,
    `action_taken` ENUM('BLOCKED', 'EMERGENCY_OVERRIDE', 'DISMISSED') NOT NULL DEFAULT 'BLOCKED',
    `event_timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_restriction_event_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_restriction_event_user` (`user_id`, `event_timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Emergency Requests Table
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

-- 11. Integrations Table (WhatsApp Business Cloud API)
DROP TABLE IF EXISTS `integrations`;
CREATE TABLE `integrations` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(64) NOT NULL,
    `integration_type` VARCHAR(64) NOT NULL,
    `connection_status` ENUM('NOT_CONNECTED', 'CONNECTING', 'CONNECTED', 'CONNECTION_FAILED', 'DISCONNECTED') DEFAULT 'NOT_CONNECTED',
    `phone_number` VARCHAR(32) DEFAULT NULL,
    `configuration_reference` TEXT DEFAULT NULL,
    `last_synced_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_integrations_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_integration` (`user_id`, `integration_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
