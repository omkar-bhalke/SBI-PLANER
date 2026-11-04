const express = require('express');
const router = express.Router();
const { requireAuth } = require('../middleware/auth');

const authController = require('../controllers/authController');
const transactionController = require('../controllers/transactionController');
const dashboardController = require('../controllers/dashboardController');
const categoryController = require('../controllers/categoryController');
const settingsController = require('../controllers/settingsController');
const aiController = require('../controllers/aiController');
const integrationController = require('../controllers/integrationController');

// 1. Authentication
router.post('/auth/google', authController.googleLogin);
router.get('/auth/profile', requireAuth, authController.getProfile);

// 2. Dashboard & Analytics
router.get('/dashboard/summary', requireAuth, dashboardController.getSummary);
router.get('/dashboard/analytics', requireAuth, dashboardController.getAnalytics);

// 3. Transactions CRUD & Offline Sync
router.get('/transactions', requireAuth, transactionController.getTransactions);
router.post('/transactions', requireAuth, transactionController.createTransaction);
router.put('/transactions/:id', requireAuth, transactionController.updateTransaction);
router.delete('/transactions/:id', requireAuth, transactionController.deleteTransaction);
router.post('/transactions/sync-batch', requireAuth, transactionController.syncBatch);

// 4. Categories
router.get('/categories', requireAuth, categoryController.getCategories);
router.post('/categories', requireAuth, categoryController.createCategory);
router.put('/categories/:id', requireAuth, categoryController.updateCategory);
router.delete('/categories/:id', requireAuth, categoryController.deleteCategory);

// 5. Settings & Limits
router.get('/settings', requireAuth, settingsController.getSettings);
router.put('/settings', requireAuth, settingsController.updateSettings);

// 6. AI Assistant & Future Emergency Approval
router.post('/ai/analyze', requireAuth, aiController.analyzeSpending);
router.post('/ai/emergency-review', requireAuth, aiController.reviewEmergencyRequest);

// 7. Integrations (WhatsApp, Bank)
router.get('/integrations', requireAuth, integrationController.getIntegrations);
router.post('/integrations/whatsapp/connect', requireAuth, integrationController.connectWhatsApp);
router.delete('/integrations/whatsapp/disconnect', requireAuth, integrationController.disconnectWhatsApp);

module.exports = router;
