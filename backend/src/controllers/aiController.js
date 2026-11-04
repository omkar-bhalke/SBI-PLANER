const crypto = require('crypto');
const db = require('../config/db');

// Secure AES-256-CBC Encryption for stored AI keys at rest
const ENCRYPTION_KEY = (process.env.ENCRYPTION_KEY || 'finpulse_encryption_secret_key_32b!').padEnd(32, '!').slice(0, 32);
const IV_LENGTH = 16;

function encryptKey(text) {
  const iv = crypto.randomBytes(IV_LENGTH);
  const cipher = crypto.createCipheriv('aes-256-cbc', Buffer.from(ENCRYPTION_KEY), iv);
  let encrypted = cipher.update(text);
  encrypted = Buffer.concat([encrypted, cipher.final()]);
  return iv.toString('hex') + ':' + encrypted.toString('hex');
}

function decryptKey(text) {
  try {
    const textParts = text.split(':');
    const iv = Buffer.from(textParts.shift(), 'hex');
    const encryptedText = Buffer.from(textParts.join(':'), 'hex');
    const decipher = crypto.createDecipheriv('aes-256-cbc', Buffer.from(ENCRYPTION_KEY), iv);
    let decrypted = decipher.update(encryptedText);
    decrypted = Buffer.concat([decrypted, decipher.final()]);
    return decrypted.toString();
  } catch (err) {
    return null;
  }
}

// Live validation against official Gemini / OpenAI endpoints
async function testAiApiKey(provider, apiKey) {
  if (provider === 'GEMINI') {
    // Ping Gemini 1.5 Flash models endpoint
    const url = `https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`;
    const response = await fetch(url, { method: 'GET' });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      const msg = errBody.error?.message || `HTTP ${response.status} from Google Gemini API`;
      return { valid: false, message: msg };
    }
    return { valid: true, message: 'Google Gemini API key validated successfully.' };
  } else if (provider === 'OPENAI') {
    // Ping OpenAI models endpoint
    const response = await fetch('https://api.openai.com/v1/models', {
      method: 'GET',
      headers: { 'Authorization': `Bearer ${apiKey}` }
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      const msg = errBody.error?.message || `HTTP ${response.status} from OpenAI API`;
      return { valid: false, message: msg };
    }
    return { valid: true, message: 'OpenAI API key validated successfully.' };
  } else {
    return { valid: false, message: 'Unsupported AI provider.' };
  }
}

// ==========================================
// TASK 4: VALIDATE & SAVE USER AI CONFIG
// ==========================================
exports.validateAndSaveConfig = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { provider = 'GEMINI', apiKey } = req.body;

    if (!apiKey || !apiKey.trim()) {
      return res.status(400).json({ success: false, message: 'API key cannot be empty.' });
    }

    const cleanKey = apiKey.trim();
    const upperProvider = provider.toUpperCase();

    // 1. Live Validation against the AI Provider
    const testResult = await testAiApiKey(upperProvider, cleanKey);
    if (!testResult.valid) {
      return res.status(400).json({
        success: false,
        code: 'INVALID_AI_KEY',
        message: `API Key Validation Failed: ${testResult.message}`
      });
    }

    // 2. Encrypt at rest on backend
    const encryptedKey = encryptKey(cleanKey);

    // 3. Upsert into user_ai_configs
    await db.query(
      `INSERT INTO user_ai_configs (id, user_id, provider, encrypted_api_key, key_status, last_validated_at)
       VALUES (?, ?, ?, ?, 'VALID', CURRENT_TIMESTAMP)
       ON DUPLICATE KEY UPDATE 
          provider = VALUES(provider),
          encrypted_api_key = VALUES(encrypted_api_key),
          key_status = 'VALID',
          last_validated_at = CURRENT_TIMESTAMP`,
      [require('uuid').v4(), userId, upperProvider, encryptedKey]
    );

    // Update active provider in user_settings
    await db.query('UPDATE user_settings SET ai_provider = ? WHERE user_id = ?', [upperProvider, userId]);

    return res.json({
      success: true,
      message: `${upperProvider} API key validated and securely saved.`,
      config: {
        provider: upperProvider,
        isConfigured: true,
        keyStatus: 'VALID',
        lastValidatedAt: new Date().toISOString()
      }
    });
  } catch (error) {
    console.error('Validate and save AI key error:', error);
    return res.status(500).json({ success: false, message: 'Error configuring AI provider: ' + error.message });
  }
};

exports.getAiConfig = async (req, res) => {
  try {
    const userId = req.user.userId;
    const [configs] = await db.query(
      'SELECT provider, key_status, last_validated_at, updated_at FROM user_ai_configs WHERE user_id = ? LIMIT 1',
      [userId]
    );

    if (configs.length === 0) {
      return res.json({
        success: true,
        config: {
          provider: 'GEMINI',
          isConfigured: false,
          keyStatus: 'UNCHECKED',
          lastValidatedAt: null
        }
      });
    }

    const cfg = configs[0];
    return res.json({
      success: true,
      config: {
        provider: cfg.provider,
        isConfigured: true,
        keyStatus: cfg.key_status,
        lastValidatedAt: cfg.last_validated_at
      }
    });
  } catch (error) {
    console.error('Get AI config error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

exports.deleteAiConfig = async (req, res) => {
  try {
    const userId = req.user.userId;
    await db.query('DELETE FROM user_ai_configs WHERE user_id = ?', [userId]);
    return res.json({ success: true, message: 'AI configuration removed.' });
  } catch (error) {
    console.error('Delete AI config error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

// ==========================================
// TASK 4: REAL AI FINANCIAL ANALYSIS
// ==========================================
exports.analyzeSpending = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { customQuery } = req.body;

    // 1. Fetch user's saved AI config
    const [configs] = await db.query(
      'SELECT provider, encrypted_api_key FROM user_ai_configs WHERE user_id = ? LIMIT 1',
      [userId]
    );

    let apiKey = null;
    let provider = 'GEMINI';

    if (configs.length > 0) {
      provider = configs[0].provider;
      apiKey = decryptKey(configs[0].encrypted_api_key);
    } else if (process.env.GEMINI_API_KEY) {
      apiKey = process.env.GEMINI_API_KEY;
      provider = 'GEMINI';
    }

    // 2. Fetch actual user transactions
    const [transactions] = await db.query(
      `SELECT transaction_type, amount, description, category_name, transaction_date 
       FROM transactions WHERE user_id = ? 
       ORDER BY transaction_date DESC LIMIT 100`,
      [userId]
    );

    if (transactions.length === 0) {
      return res.json({
        success: true,
        analysis: "Your financial ledger is currently empty. Once you log your everyday income and expenses (such as tea, meals, books, printing, or transit), I will analyze your spending patterns, flag repeated purchases, and guide your savings!"
      });
    }

    // Compute ledger statistics
    let totalExpense = 0;
    let totalIncome = 0;
    const categoryTotals = {};
    const itemFrequencies = {};

    transactions.forEach(t => {
      const amt = parseFloat(t.amount);
      if (t.transaction_type === 'EXPENSE') {
        totalExpense += amt;
        categoryTotals[t.category_name] = (categoryTotals[t.category_name] || 0) + amt;
        const desc = t.description.toLowerCase().trim();
        if (!itemFrequencies[desc]) {
          itemFrequencies[desc] = { count: 0, total: 0, original: t.description };
        }
        itemFrequencies[desc].count += 1;
        itemFrequencies[desc].total += amt;
      } else {
        totalIncome += amt;
      }
    });

    const sortedCategories = Object.entries(categoryTotals).sort((a, b) => b[1] - a[1]);
    const topCategory = sortedCategories[0] || ['None', 0];
    const repeatedItems = Object.values(itemFrequencies).filter(i => i.count >= 2).sort((a, b) => b.total - a.total);

    // If user has NOT configured an AI provider API key, inform them to configure one
    if (!apiKey) {
      return res.json({
        success: true,
        requiresConfiguration: true,
        analysis: `⚠️ **AI Key Not Configured**\n\n` +
          `You have not configured your Google Gemini or OpenAI API key yet in Settings.\n\n` +
          `• Ledger snapshot: ${transactions.length} items logged (Total spent: ₹${totalExpense.toFixed(0)}, Top category: ${topCategory[0]}).\n\n` +
          `Please go to **Settings > AI Engine Configuration** and enter your API key to activate live personalized financial insights and advisory.`,
        summary: {
          totalExpense,
          totalIncome,
          topCategory: topCategory[0],
          topCategoryAmount: topCategory[1],
          repeatedItemsCount: repeatedItems.length
        }
      });
    }

    // Construct detailed prompt based on actual transaction ledger
    const ledgerSummaryText = `
User Financial Ledger Summary:
- Total Outflow (Expenses): ₹${totalExpense.toFixed(2)} across ${transactions.filter(t => t.transaction_type === 'EXPENSE').length} expense transactions
- Total Inflow (Income): ₹${totalIncome.toFixed(2)}
- Expense Breakdown by Category: ${sortedCategories.map(([cat, amt]) => `${cat}: ₹${amt.toFixed(2)}`).join(', ')}
- Repeated Micro-Purchases: ${repeatedItems.map(i => `"${i.original}" purchased ${i.count} times (Total: ₹${i.total.toFixed(2)})`).join(', ') || 'None'}
- Recent Transaction Samples: ${transactions.slice(0, 10).map(t => `${t.transaction_date}: ${t.description} (₹${t.amount}, ${t.category_name})`).join('; ')}
User Question/Query: "${customQuery || 'Provide a comprehensive financial health assessment, highlight repeated micro-purchases, unnecessary expenses, and actionable savings steps.'}"
`.trim();

    let aiGeneratedResponse = null;

    if (provider === 'GEMINI') {
      const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`;
      const payload = {
        contents: [{
          role: 'user',
          parts: [{
            text: `You are FinPulse AI, a professional financial discipline advisor. Analyze the user's REAL ledger data below and answer their question with high accuracy, specific numbers, and disciplined budgeting advice. Do not hallucinate numbers outside the provided ledger.\n\n${ledgerSummaryText}`
          }]
        }]
      };

      const geminiRes = await fetch(geminiUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!geminiRes.ok) {
        const errJson = await geminiRes.json().catch(() => ({}));
        const errDetail = errJson.error?.message || `HTTP ${geminiRes.status}`;
        return res.status(502).json({
          success: false,
          code: 'AI_PROVIDER_ERROR',
          message: `Google Gemini API returned an error: ${errDetail}. Please check your API key in Settings.`
        });
      }

      const geminiData = await geminiRes.json();
      aiGeneratedResponse = geminiData.candidates?.[0]?.content?.parts?.[0]?.text;
    } else if (provider === 'OPENAI') {
      const openAiUrl = 'https://api.openai.com/v1/chat/completions';
      const payload = {
        model: 'gpt-4o-mini',
        messages: [
          {
            role: 'system',
            content: 'You are FinPulse AI, a professional personal finance advisor. Analyze the real transaction ledger data provided and give disciplined, accurate financial advice.'
          },
          { role: 'user', content: ledgerSummaryText }
        ],
        temperature: 0.7
      };

      const openAiRes = await fetch(openAiUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${apiKey}`
        },
        body: JSON.stringify(payload)
      });

      if (!openAiRes.ok) {
        const errJson = await openAiRes.json().catch(() => ({}));
        const errDetail = errJson.error?.message || `HTTP ${openAiRes.status}`;
        return res.status(502).json({
          success: false,
          code: 'AI_PROVIDER_ERROR',
          message: `OpenAI API returned an error: ${errDetail}. Please check your API key in Settings.`
        });
      }

      const openAiData = await openAiRes.json();
      aiGeneratedResponse = openAiData.choices?.[0]?.message?.content;
    }

    if (!aiGeneratedResponse) {
      return res.status(502).json({
        success: false,
        message: 'The configured AI service did not return a valid analytical response.'
      });
    }

    return res.json({
      success: true,
      provider,
      analysis: aiGeneratedResponse,
      summary: {
        totalExpense,
        totalIncome,
        topCategory: topCategory[0],
        topCategoryAmount: topCategory[1],
        repeatedItemsCount: repeatedItems.length
      }
    });
  } catch (error) {
    console.error('AI Analysis error:', error);
    return res.status(500).json({ success: false, message: 'AI processing failed: ' + error.message });
  }
};

exports.reviewEmergencyRequest = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { requestedAmount, reason, screenshotUrl } = req.body;

    if (!requestedAmount || !reason) {
      return res.status(400).json({ success: false, message: 'Requested amount and reason are required.' });
    }

    const isEducationOrMedical = /print|assignment|exam|project|xerox|medicine|doctor|clinic|hospital|study|transit|emergency|travel/i.test(reason);
    const recommendation = isEducationOrMedical
      ? `Expense appears essential and time-sensitive (${reason.trim()}). Recommendation: Grant temporary daily spending allowance extension of ₹${parseFloat(requestedAmount).toFixed(0)} upon explicit user review.`
      : `Expense appears discretionary. Recommendation: Defer this non-essential purchase until tomorrow's fresh daily spending limit reset.`;

    return res.json({
      success: true,
      recommendation,
      confidenceScore: isEducationOrMedical ? 0.95 : 0.40,
      suggestedApproval: isEducationOrMedical,
      requiresUserConfirmation: true
    });
  } catch (error) {
    console.error('Emergency review error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};
