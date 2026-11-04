-- =====================================================================
-- FinPulse Database Migration v2
-- Upgrade: Email/Password Auth, One Device = One Account, Real AI Configs,
-- and App Restriction Tracking
-- =====================================================================

-- 1. Modify users table for email/password authentication
ALTER TABLE `users` 
    MODIFY COLUMN `google_id` VARCHAR(128) NULL,
    ADD COLUMN `password_hash` VARCHAR(255) NULL AFTER `email`,
    ADD COLUMN `is_email_verified` BOOLEAN DEFAULT TRUE AFTER `password_hash`,
    ADD COLUMN `verification_otp` VARCHAR(16) NULL AFTER `is_email_verified`,
    ADD COLUMN `otp_expires_at` TIMESTAMP NULL AFTER `verification_otp`,
    ADD COLUMN `reset_password_otp` VARCHAR(16) NULL AFTER `otp_expires_at`,
    ADD COLUMN `reset_otp_expires_at` TIMESTAMP NULL AFTER `reset_password_otp`;

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

-- 3. User Real AI Configurations Table (Encrypted at rest)
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

-- 4. App Restrictions Configuration Table (Selected Payment Apps to restrict)
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

-- 5. App Restriction Interception Events Table
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
