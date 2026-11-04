const db = require('../config/db');

exports.analyzeSpending = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { customQuery } = req.body;

    // Fetch actual user transactions
    const [transactions] = await db.query(
      `SELECT transaction_type, amount, description, category_name, transaction_date 
       FROM transactions WHERE user_id = ? 
       ORDER BY transaction_date DESC LIMIT 100`,
      [userId]
    );

    if (transactions.length === 0) {
      return res.json({
        success: true,
        analysis: "You don't have any recorded transactions yet. Once you log your daily expenses (like tea, meals, printing, or transportation), I will analyze your spending habits, highlight repeated costs, and help you save money!"
      });
    }

    // Compute key actual statistics
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

    // Sort categories
    const sortedCategories = Object.entries(categoryTotals).sort((a, b) => b[1] - a[1]);
    const topCategory = sortedCategories[0] || ['None', 0];

    // Find repeated items
    const repeatedItems = Object.values(itemFrequencies)
      .filter(i => i.count >= 2)
      .sort((a, b) => b.total - a.total);

    let repeatedHighlight = '';
    if (repeatedItems.length > 0) {
      const topRepeated = repeatedItems[0];
      repeatedHighlight = ` You frequently purchase "${topRepeated.original}" (${topRepeated.count} times totaling ₹${topRepeated.total.toFixed(0)}).`;
    }

    const savingsPotential = (totalExpense * 0.15).toFixed(0);

    let analysisMessage = `📊 **Financial Health Assessment**:\n\n` +
      `• **Total Tracked Expenses**: ₹${totalExpense.toFixed(0)} across ${transactions.filter(t => t.transaction_type === 'EXPENSE').length} transactions.\n` +
      `• **Primary Spending Category**: **${topCategory[0]}** accounts for ₹${topCategory[1].toFixed(0)} (${((topCategory[1] / (totalExpense || 1)) * 100).toFixed(0)}% of expenses).\n` +
      (repeatedHighlight ? `• **Frequent Micro-Purchases**:${repeatedHighlight}\n` : '') +
      `• **Actionable Discipline Tip**: By setting a strict micro-budget on discretionary expenses and capping daily snacks/dining, you can realistically retain **₹${savingsPotential}** this month!\n\n` +
      `Keep logging your transactions diligently to keep your spending within your daily limit.`;

    if (customQuery) {
      analysisMessage = `Based on your recent transaction ledger of ${transactions.length} items (Total spent: ₹${totalExpense.toFixed(0)}):\n\n` +
        `Regarding your question: "${customQuery}"\n\n` +
        `Your top spending is in **${topCategory[0]}** (₹${topCategory[1].toFixed(0)}).` +
        (repeatedItems.length > 0 ? ` Repeated costs like "${repeatedItems[0].original}" (₹${repeatedItems[0].total.toFixed(0)}) have the biggest potential for savings.` : '') +
        ` Maintaining your daily allowance is key to meeting your savings goal!`;
    }

    return res.json({
      success: true,
      analysis: analysisMessage,
      summary: {
        totalExpense,
        totalIncome,
        topCategory: topCategory[0],
        topCategoryAmount: topCategory[1],
        repeatedItemsCount: repeatedItems.length,
        potentialSavings: parseFloat(savingsPotential)
      }
    });
  } catch (error) {
    console.error('AI Analysis error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

exports.reviewEmergencyRequest = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { requestedAmount, reason, screenshotUrl } = req.body;

    if (!requestedAmount || !reason) {
      return res.status(400).json({ success: false, message: 'Requested amount and reason are required' });
    }

    const isEducationOrMedical = /print|assignment|exam|project|medicine|doctor|clinic|hospital|study|travel/i.test(reason);
    const recommendation = isEducationOrMedical
      ? "Expense appears essential and time-sensitive. Recommended temporary spending exception of ₹" + requestedAmount + " valid for 2 hours upon user confirmation."
      : "Expense appears discretionary. Recommend deferring until tomorrow's fresh daily spending limit cycle.";

    return res.json({
      success: true,
      recommendation,
      confidenceScore: isEducationOrMedical ? 0.92 : 0.45,
      suggestedApproval: isEducationOrMedical,
      requiresUserConfirmation: true
    });
  } catch (error) {
    console.error('Emergency review error:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};
