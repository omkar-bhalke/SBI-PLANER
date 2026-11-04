const mysql = require('mysql2/promise');
require('dotenv').config();

const pool = mysql.createPool({
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '3306', 10),
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'finpulse_db',
  waitForConnections: true,
  connectionLimit: parseInt(process.env.DB_CONNECTION_LIMIT || '10', 10),
  queueLimit: 0,
  decimalNumbers: true,
  timezone: '+00:00'
});

pool.getConnection()
  .then((conn) => {
    console.log('✅ Connected to MySQL database successfully');
    conn.release();
  })
  .catch((err) => {
    console.warn('⚠️ MySQL connection notice (will retry on incoming requests):', err.message);
  });

module.exports = pool;
