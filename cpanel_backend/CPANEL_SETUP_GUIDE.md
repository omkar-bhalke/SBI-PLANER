# FinPulse cPanel Deployment & Database Setup Guide
**Domain**: `https://radhewins.buzz`
**Compatibility**: Apache 2.4+, LiteSpeed, Nginx reverse proxy, MySQL 5.7+, MySQL 8.0+, MariaDB 10.3+

---

## 📋 Step 1: Create the MySQL Database in cPanel

1. Log into your cPanel account for **`radhewins.buzz`**.
2. Scroll to the **Databases** section and click on **MySQL Databases** (or **MySQL Database Wizard**).
3. **Create New Database**:
   - Enter a database name (for example, `radhewin_finpulse`).
   - Click **Create Database**.
4. **Create Database User**:
   - Scroll down to **MySQL Users -> Add New User**.
   - Enter a username (for example, `radhewin_dbuser`).
   - Enter a strong password or generate one using the Password Generator. **Copy this password!**
   - Click **Create User**.
5. **Assign User to Database**:
   - Under **Add User To Database**, select the user you just created (`radhewin_dbuser`) and the database (`radhewin_finpulse`).
   - Click **Add**.
   - Check the box **ALL PRIVILEGES**.
   - Click **Make Changes**.

---

## 🗄️ Step 2: Import the SQL Schema in phpMyAdmin

1. In cPanel, click **phpMyAdmin** (under the Databases section).
2. On the left sidebar, click on your newly created database (`radhewin_finpulse`).
3. Click on the **Import** tab at the top.
4. Click **Choose File** and select `database.sql` from the `cpanel_backend` folder.
5. Ensure the character set is **utf8mb4**.
6. Scroll down and click **Import** (or **Go**).
7. You should see a green success banner: *"Import has been successfully finished, 12 tables created."*

---

## 📁 Step 3: Upload Backend Files via cPanel File Manager

1. In cPanel, open **File Manager** (under Files).
2. Navigate to your root web folder:
   - For your main domain `https://radhewins.buzz`, this is **`public_html/`**.
   - *(Note: If you already have a website on `radhewins.buzz` and only want the API in an `api` subfolder, upload the files inside `public_html/api/`)*.
3. Upload all files and folders from `cpanel_backend/`:
   ```
   public_html/
   ├── .htaccess
   ├── index.php
   ├── config/
   │   ├── db.php
   │   └── cors.php
   ├── helpers/
   │   ├── Jwt.php
   │   ├── Response.php
   │   └── Validator.php
   ├── middleware/
   │   └── AuthMiddleware.php
   └── controllers/
       ├── AuthController.php
       ├── TransactionController.php
       ├── DashboardController.php
       ├── SettingsController.php
       ├── AiController.php
       ├── IntegrationController.php
       └── RestrictionController.php
   ```
4. **Ensure hidden files are visible**:
   - In File Manager, click **Settings** (top-right corner).
   - Check **Show Hidden Files (dotfiles)** and click **Save**.
   - Verify that **`.htaccess`** is present in the folder.

---

## ⚙️ Step 4: Configure Database Credentials

1. In cPanel File Manager, open the **`config/`** folder.
2. Select **`db.php`** and click **Edit** (top toolbar).
3. Update the credentials with the exact values you created in Step 1:
   ```php
   define('DB_HOST', 'localhost'); // Keep as localhost
   define('DB_NAME', 'radhewin_finpulse'); // Your full cPanel DB name
   define('DB_USER', 'radhewin_dbuser');   // Your full cPanel DB user
   define('DB_PASS', 'YOUR_ACTUAL_STRONG_PASSWORD'); // Your DB password
   ```
4. Click **Save Changes**.

---

## 🧪 Step 5: Verify Your API Live in Browser

Open your web browser and visit:
👉 **`https://radhewins.buzz/api/health`** (or `https://radhewins.buzz/health`)

You will see the JSON response:
```json
{
  "success": true,
  "message": "FinPulse backend is online and connected to cPanel MySQL.",
  "service": "FinPulse cPanel REST API",
  "domain": "https://radhewins.buzz",
  "status": "OPERATIONAL",
  "version": "2.1.0",
  "serverTime": "2026-11-04 11:45:00 UTC"
}
```

If you see this response, your cPanel backend and MySQL database are active and operational!

---

## 📱 Step 6: Connect the Android Application

The Android app is pre-configured with:
- **Base URL**: `https://radhewins.buzz/`
- **Keystore Device Identity**: Enforced via `X-Device-Id` HTTP header.
- **Permanent Cloud Sync**: Automatic two-way synchronization on every transaction and full cloud restoration on login/reinstall.

You can now register your account in the FinPulse Android app. It will directly create records in your cPanel MySQL database!
