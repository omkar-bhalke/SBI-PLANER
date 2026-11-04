<?php
/**
 * Pure PHP JWT (JSON Web Token) and Cryptographic Helper
 * Requires no Composer packages - operates natively on standard PHP 7.4 / 8.0 / 8.1 / 8.2 / 8.3 in cPanel.
 */

class Jwt {
    private static function base64UrlEncode(string $data): string {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    private static function base64UrlDecode(string $data): string {
        return base64_decode(strtr($data, '-_', '+/') . str_repeat('=', 3 - (3 + strlen($data)) % 4));
    }

    public static function generateToken(array $payload, int $ttlSeconds = 604800): string { // default 7 days
        $header = ['typ' => 'JWT', 'alg' => 'HS256'];
        $now = time();
        $payload['iat'] = $now;
        $payload['exp'] = $now + $ttlSeconds;

        $base64Header = self::base64UrlEncode(json_encode($header));
        $base64Payload = self::base64UrlEncode(json_encode($payload));

        $signature = hash_hmac('sha256', "$base64Header.$base64Payload", JWT_SECRET_KEY, true);
        $base64Signature = self::base64UrlEncode($signature);

        return "$base64Header.$base64Payload.$base64Signature";
    }

    public static function generateRefreshToken(string $userId, string $deviceId): string {
        return bin2hex(random_bytes(32)) . "_" . dechex(time());
    }

    public static function verifyToken(string $jwt): ?array {
        $parts = explode('.', $jwt);
        if (count($parts) !== 3) {
            return null;
        }

        list($base64Header, $base64Payload, $base64Signature) = $parts;

        $expectedSignature = self::base64UrlEncode(
            hash_hmac('sha256', "$base64Header.$base64Payload", JWT_SECRET_KEY, true)
        );

        if (!hash_equals($expectedSignature, $base64Signature)) {
            return null; // Tampered or invalid signature
        }

        $payload = json_decode(self::base64UrlDecode($base64Payload), true);
        if (!$payload || !isset($payload['exp'])) {
            return null;
        }

        if ($payload['exp'] < time()) {
            return null; // Expired token
        }

        return $payload;
    }

    // AES-256-CBC Encryption for AI Keys stored at rest in MySQL
    public static function encryptAes256(string $plaintext): string {
        $cipher = "aes-256-cbc";
        $key = hash('sha256', AES_ENCRYPTION_KEY, true);
        $iv = random_bytes(openssl_cipher_iv_length($cipher));
        $encrypted = openssl_encrypt($plaintext, $cipher, $key, 0, $iv);
        return base64_encode($iv . $encrypted);
    }

    public static function decryptAes256(string $ciphertext): ?string {
        $cipher = "aes-256-cbc";
        $raw = base64_decode($ciphertext);
        $ivLen = openssl_cipher_iv_length($cipher);
        if (strlen($raw) <= $ivLen) {
            return null;
        }
        $iv = substr($raw, 0, $ivLen);
        $encrypted = substr($raw, $ivLen);
        $key = hash('sha256', AES_ENCRYPTION_KEY, true);
        $decrypted = openssl_decrypt($encrypted, $cipher, $key, 0, $iv);
        return $decrypted !== false ? $decrypted : null;
    }
}
