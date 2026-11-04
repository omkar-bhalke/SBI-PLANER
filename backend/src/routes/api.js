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
const restrictionController = require('../controllers/restrictionController');

// 1. Authentication & Device Binding (Tasks 1 & 2)
router.post('/auth/register', authController.register);
router.post('/auth/login', authController.login);
router.post('/auth/forgot-password', authController.forgotPassword);
router.post('/auth/reset-password', authController.resetPassword);
router.post('/auth/refresh-token', authController.refreshToken);
router.get('/auth/device-status', authController.checkDeviceStatus);
router.post('/auth/device-reset', authController.resetDevice);
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

// 6. Real AI Assistant & Provider Configuration (Task 4)
router.get('/ai/config', requireAuth, aiController.getAiConfig);
router.post('/ai/validate-and-save', requireAuth, aiController.validateAndSaveConfig);
router.delete('/ai/config', requireAuth, aiController.deleteAiConfig);
router.post('/ai/analyze', requireAuth, aiController.analyzeSpending);
router.post('/ai/emergency-review', requireAuth, aiController.reviewEmergencyRequest);

// 7. Real WhatsApp Integrations (Task 3)
router.get('/integrations', requireAuth, integrationController.getIntegrations);
router.get('/integrations/whatsapp/status', requireAuth, integrationController.getWhatsAppStatus);
router.post('/integrations/whatsapp/connect', requireAuth, integrationController.connectWhatsApp);
router.delete('/integrations/whatsapp/disconnect', requireAuth, integrationController.disconnectWhatsApp);

// 8. Real App Restrictions & Accessibility Logging (Task 5)
router.get('/restrictions', requireAuth, restrictionController.getRestrictions);
router.post('/restrictions', requireAuth, restrictionController.updateRestrictions);
router.post('/restrictions/log-event', requireAuth, restrictionController.logRestrictionEvent);

module.exports = router;
