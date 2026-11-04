# FinPulse REST API Documentation
**Base URL**: `https://radhewins.buzz/`

All API endpoints accept and return `application/json`.
Authenticated requests require:
- `Authorization: Bearer <jwt_token>`
- `X-Device-Id: <hardware_device_id>`

---

## 1. Authentication & Device Binding

### Register User & Bind Hardware Device
- **Endpoint**: `POST /api/auth/register`
- **Request Body**:
```json
{
  "name": "Omkar Bhalke",
  "email": "bhalkeomkar49@gmail.com",
  "password": "StrongPassword@2026",
  "confirmPassword": "StrongPassword@2026",
  "deviceId": "FP-GOO-4a7b9c1d2e3f-9876543210abcdef",
  "deviceFingerprint": "b78a9c1d2e3f4a5b6c7d8e9f0a1b2c3d",
  "deviceModel": "Google Pixel 8",
  "osVersion": "Android 14 (API 34)"
}
```
- **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Registration successful and device bound.",
  "token": "eyJ0eXAi...",
  "refreshToken": "7a8b9c0d1e...",
  "isFirstTime": true,
  "user": {
    "id": "usr_9823471029",
    "email": "bhalkeomkar49@gmail.com",
    "name": "Omkar Bhalke",
    "profileImage": null
  }
}
```
- **Error (409 Conflict - Device already bound)**:
```json
{
  "success": false,
  "message": "This device is already registered to account (bh***@gmail.com). FinPulse allows strictly one account per physical device.",
  "code": "DEVICE_ALREADY_BOUND"
}
```

### Login
- **Endpoint**: `POST /api/auth/login`
- **Request Body**:
```json
{
  "email": "bhalkeomkar49@gmail.com",
  "password": "StrongPassword@2026",
  "deviceId": "FP-GOO-4a7b9c1d2e3f-9876543210abcdef",
  "deviceFingerprint": "b78a9c1d2e3f4a5b6c7d8e9f0a1b2c3d"
}
```

### Forgot Password (OTP Dispatch)
- **Endpoint**: `POST /api/auth/forgot-password`
- **Request Body**:
```json
{
  "email": "bhalkeomkar49@gmail.com"
}
```

### Reset Password with OTP
- **Endpoint**: `POST /api/auth/reset-password`
- **Request Body**:
```json
{
  "email": "bhalkeomkar49@gmail.com",
  "otp": "492019",
  "newPassword": "NewStrongPassword@2026",
  "confirmPassword": "NewStrongPassword@2026"
}
```

### Check Device Status
- **Endpoint**: `GET /api/auth/device-status?deviceId=FP-GOO-4a7b9c1d...`

### Device Reset / Unbind
- **Endpoint**: `POST /api/auth/device-reset`
- **Request Body**:
```json
{
  "email": "bhalkeomkar49@gmail.com",
  "password": "StrongPassword@2026",
  "deviceId": "FP-GOO-4a7b9c1d..."
}
```

---

## 2. Dashboard & Financial Summaries

### Get Complete Dashboard Summary
- **Endpoint**: `GET /api/dashboard/summary`
- **Headers**: `Authorization: Bearer <token>`, `X-Device-Id: <deviceId>`
- **Response**:
```json
{
  "success": true,
  "balance": {
    "currentBalance": 14500.0,
    "initialBalance": 15000.0,
    "totalIncome": 5000.0,
    "totalExpenses": 5500.0,
    "bankName": "State Bank of India (SBI)"
  },
  "today": {
    "income": 0.0,
    "expense": 240.0,
    "dailyLimit": 500.0,
    "remainingAllowance": 260.0,
    "utilizationPercentage": 48.0,
    "isExceeded": false
  },
  "monthly": {
    "income": 5000.0,
    "expense": 5500.0,
    "savings": 0.0,
    "spendingPercentage": 100.0
  },
  "recentTransactions": [...]
}
```

---

## 3. Transactions & Synchronization

### List Transactions
- **Endpoint**: `GET /api/transactions?page=1&limit=50`

### Create Transaction
- **Endpoint**: `POST /api/transactions`
- **Request Body**:
```json
{
  "amount": 150.0,
  "type": "EXPENSE",
  "description": "Lunch at cafe",
  "categoryName": "Food & Dining",
  "transactionDate": "2026-11-04",
  "transactionTime": "13:30:00",
  "timestamp": 1793883000000,
  "notes": "Paid via UPI",
  "idempotencyKey": "tx_cli_98234827"
}
```

### Batch Sync (Offline-to-Cloud)
- **Endpoint**: `POST /api/transactions/sync-batch`
- **Request Body**:
```json
{
  "pendingTransactions": [
    {
      "clientLocalId": "local_101",
      "amount": 20.0,
      "type": "EXPENSE",
      "description": "Tea",
      "categoryName": "Food & Dining",
      "transactionDate": "2026-11-04",
      "transactionTime": "10:15:00",
      "idempotencyKey": "idem_101"
    }
  ]
}
```

### Pull All Cloud Data (Recovery on Reinstall)
- **Endpoint**: `GET /api/user/sync-all` (or `GET /api/transactions/all`)
- Returns all user transactions, categories, daily limit, balance, and WhatsApp configurations to completely rebuild local Room database.

---

## 4. Real AI Configuration & Assistant

### Validate and Save AI Key
- **Endpoint**: `POST /api/ai/validate-and-save`
- **Request Body**:
```json
{
  "provider": "GEMINI",
  "apiKey": "AIzaSy..."
}
```

### Analyze Spending (Grounded in Transactions)
- **Endpoint**: `POST /api/ai/analyze`
- **Request Body**:
```json
{
  "customQuery": "Where did I spend the most this week?"
}
```

### Emergency Expense Review
- **Endpoint**: `POST /api/ai/emergency-review`
- **Request Body**:
```json
{
  "requestedAmount": 1500.0,
  "reason": "Semester exam fees and medical prescription"
}
```

---

## 5. Meta WhatsApp Business Cloud API

### WhatsApp Status
- **Endpoint**: `GET /api/integrations/whatsapp/status`

### Connect WhatsApp Gateway
- **Endpoint**: `POST /api/integrations/whatsapp/connect`
- **Request Body**:
```json
{
  "phoneNumber": "+91 9876543210",
  "accessToken": "EAA...",
  "phoneNumberId": "1098234...",
  "wabaId": "981234..."
}
```

### Disconnect WhatsApp
- **Endpoint**: `DELETE /api/integrations/whatsapp/disconnect`

---

## 6. App Restrictions & Budget Enforcement

### Get Monitored Payment Apps
- **Endpoint**: `GET /api/restrictions`

### Update Monitored Apps
- **Endpoint**: `POST /api/restrictions`
- **Request Body**:
```json
{
  "restrictions": [
    {"packageName": "com.phonepe.app", "appName": "PhonePe", "isEnabled": true},
    {"packageName": "com.google.android.apps.nbu.paisa.user", "appName": "Google Pay", "isEnabled": true}
  ]
}
```

### Log Overspending Interception Event
- **Endpoint**: `POST /api/restrictions/log-event`
- **Request Body**:
```json
{
  "packageName": "com.phonepe.app",
  "appName": "PhonePe",
  "dailyLimit": 500.0,
  "amountSpent": 650.0,
  "amountExceeded": 150.0,
  "actionTaken": "BLOCKED"
}
```
