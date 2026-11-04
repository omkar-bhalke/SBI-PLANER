<?php
/**
 * Request Validation & Input Sanitization Helper
 */

class Validator {
    public static function getJsonBody(): array {
        $raw = file_get_contents('php://input');
        if (empty($raw)) {
            return [];
        }
        $data = json_decode($raw, true);
        return is_array($data) ? $data : [];
    }

    public static function validateEmail(string $email): bool {
        return filter_var($email, FILTER_VALIDATE_EMAIL) !== false;
    }

    public static function validatePassword(string $password): array {
        if (strlen($password) < 8) {
            return [false, 'Password must be at least 8 characters long'];
        }
        return [true, ''];
    }

    public static function sanitizeString(?string $str): string {
        if ($str === null) return '';
        return trim(htmlspecialchars(strip_tags($str), ENT_QUOTES, 'UTF-8'));
    }
}
