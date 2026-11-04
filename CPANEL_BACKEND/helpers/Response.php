<?php
/**
 * Standardized JSON API Response Helper
 */

class Response {
    public static function json(bool $success, ?string $message = null, $data = null, int $statusCode = 200, ?string $code = null): void {
        http_response_code($statusCode);
        $payload = [
            'success' => $success
        ];

        if ($message !== null) {
            $payload['message'] = $message;
        }

        if ($code !== null) {
            $payload['code'] = $code;
        }

        if ($data !== null) {
            if (is_array($data)) {
                foreach ($data as $key => $value) {
                    $payload[$key] = $value;
                }
            } else {
                $payload['data'] = $data;
            }
        }

        echo json_encode($payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
        exit;
    }

    public static function success($data = null, string $message = 'Success', int $statusCode = 200): void {
        self::json(true, $message, $data, $statusCode);
    }

    public static function error(string $message, int $statusCode = 400, ?string $code = null, $extra = null): void {
        self::json(false, $message, $extra, $statusCode, $code);
    }

    public static function unauthorized(string $message = 'Unauthorized access', ?string $code = 'UNAUTHORIZED'): void {
        self::json(false, $message, null, 401, $code);
    }

    public static function notFound(string $message = 'Resource not found'): void {
        self::json(false, $message, null, 404, 'NOT_FOUND');
    }
}
