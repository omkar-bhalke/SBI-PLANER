# FinPulse - Technical Architecture & Production Deployment Guide

## 1. System Architecture Overview

FinPulse is built using a decoupled three-tier architecture:
1. **Client**: Native Android application (Kotlin, Jetpack Compose, MVVM, Room SQLite for local caching & offline support, Retrofit for REST API communication, Jetpack DataStore).
2. **Cloud Backend**: Node.js & Express.js REST API deployed on Railway, secured with Google OAuth verification and JWT session tokens.
3. **Database**: Relational MySQL 8.0+ database hosted on cPanel or cloud MySQL instance, storing monetary records with `DECIMAL(14,2)` precision.

```
┌───────────────────────────────────────┐
│     Native Android Client (Kotlin)    │
│  - Jetpack Compose UI (Material 3)    │
│  - MVVM Architecture                  │
│  - Room Local Cache (Offline Engine)  │
│  - DataStore Preferences              │
│  - Retrofit HTTP Client               │
└──────────────────┬────────────────────┘
                   │ HTTPS (JSON / REST)
                   ▼
┌───────────────────────────────────────┐
│     Cloud Backend API (Node.js)       │
│  - Express.js Router                  │
│  - Google OAuth / JWT Auth Middleware │
│  - Idempotency & Batch Sync Engine    │
│  - AI Financial Assistant Synthesis   │
│  - Railway Deployment                 │
└──────────────────┬────────────────────┘
                   │ TCP / SSL Connection
                   ▼
┌───────────────────────────────────────┐
│    Relational Database (cPanel MySQL) │
│  - users & user_settings              │
│  - accounts (SBI primary balance)     │
│  - transactions (DECIMAL precision)   │
│  - categories (default & custom)      │
│  - emergency_requests & integrations  │
└───────────────────────────────────────┘
```

---

## 2. Android Build & Execution

### Requirements
- Android SDK 36 (minSdk 24, targetSdk 36)
- Java 11 / 17
- Gradle 8+ with Kotlin 2.2+

### Running the App
- In AI Studio: Build and preview directly via the Streaming Emulator.
- From Terminal / CLI:
  ```bash
  gradle :app:assembleDebug
  ```
- Output debug APK will be generated at:
  `app/build/outputs/apk/debug/app-debug.apk`

---

## 3. Cloud Backend (Railway Deployment)

### Prerequisites
- Node.js 18+
- Railway Account (https://railway.app)

### Step-by-Step Railway Deployment:
1. Push the `/backend` directory or entire repository to GitHub.
2. In Railway, click **New Project** -> **Deploy from GitHub repo**.
3. Set the Root Directory to `/backend`.
4. Add the Environment Variables in Railway Dashboard:
   - `PORT=3000`
   - `NODE_ENV=production`
   - `DB_HOST=<your_cpanel_mysql_hostname>`
   - `DB_PORT=3306`
   - `DB_USER=<your_cpanel_db_username>`
   - `DB_PASSWORD=<your_cpanel_db_password>`
   - `DB_NAME=<your_cpanel_database_name>`
   - `JWT_SECRET=<secure_random_64_char_string>`
   - `GOOGLE_CLIENT_ID=<your_google_oauth_client_id>.apps.googleusercontent.com`
   - `GEMINI_API_KEY=<your_gemini_api_key>`
5. Deploy. Railway will run `npm start` and expose your public HTTPS URL (e.g., `https://finpulse-api.railway.app`).
6. Update the `backendUrl` in the Android app's Settings to point to your Railway URL.

---

## 4. cPanel MySQL Database Setup

1. Log into your web hosting **cPanel**.
2. Under the **Databases** section, click **MySQL® Databases**.
3. Create a new database: e.g., `cpaneluser_finpulse`.
4. Create a new MySQL user and set a strong password.
5. Add the user to the database and grant **ALL PRIVILEGES**.
6. Under **Databases**, open **phpMyAdmin**.
7. Select the `cpaneluser_finpulse` database and click the **Import** tab.
8. Upload and execute `/database/schema.sql`.
9. Next, upload and execute `/database/seeds.sql` to populate default spending categories.
10. Ensure Remote MySQL access is allowed for Railway if hosted externally (add `%` or Railway's outbound IP under **Remote MySQL** in cPanel).

---

## 5. Google Sign-In & OAuth Configuration

1. Visit [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new project or select an existing one.
3. Configure the **OAuth Consent Screen** (User Type: External, fill in app name "FinPulse" and developer email).
4. Create Credentials:
   - **Type 1: Web Application**: This generates the `GOOGLE_CLIENT_ID` used by both the backend token verification and the Credential Manager client.
   - **Type 2: Android**: Add package name `com.aistudio.finpulse.wkrxza` and your SHA-1 key fingerprint.
5. Copy the Client ID into `backend/.env` under `GOOGLE_CLIENT_ID`.

---

## 6. Offline Synchronization & Idempotency

- Every transaction created while offline is written immediately to Android's local Room database with `SyncStatus = PENDING` and a unique `idempotencyKey` UUID.
- `SyncManager` monitors `ConnectivityManager` network availability.
- When an active internet connection is detected, pending transactions are posted in batches to `POST /api/transactions/sync-batch`.
- The MySQL backend checks `idempotency_key` to guarantee zero duplicate records even on unstable connections.
- After confirmation, the local status is updated to `SYNCED`.
