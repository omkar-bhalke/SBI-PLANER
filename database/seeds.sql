-- =====================================================================
-- FinPulse - Default Categories Seed
-- =====================================================================

INSERT INTO `categories` (`id`, `user_id`, `category_name`, `category_type`, `icon_name`, `color_hex`, `is_default`) VALUES
('cat_food', NULL, 'Food & Dining', 'EXPENSE', 'restaurant', '#F59E0B', 1),
('cat_transport', NULL, 'Transportation', 'EXPENSE', 'directions_bus', '#3B82F6', 1),
('cat_education', NULL, 'Education', 'EXPENSE', 'school', '#8B5CF6', 1),
('cat_printing', NULL, 'Printing & Stationery', 'EXPENSE', 'print', '#6366F1', 1),
('cat_shopping', NULL, 'Shopping', 'EXPENSE', 'shopping_bag', '#EC4899', 1),
('cat_entertainment', NULL, 'Entertainment', 'EXPENSE', 'movie', '#10B981', 1),
('cat_recharge', NULL, 'Mobile Recharge', 'EXPENSE', 'phone_android', '#06B6D4', 1),
('cat_personal', NULL, 'Personal Care', 'EXPENSE', 'person', '#14B8A6', 1),
('cat_other_exp', NULL, 'Other Expenses', 'EXPENSE', 'more_horiz', '#64748B', 1),

-- Default Income Categories
('cat_salary', NULL, 'Salary / Allowance', 'INCOME', 'account_balance_wallet', '#10B981', 1),
('cat_scholarship', NULL, 'Scholarship', 'INCOME', 'card_giftcard', '#059669', 1),
('cat_family', NULL, 'Money from Family', 'INCOME', 'family_restroom', '#047857', 1),
('cat_investment', NULL, 'Investments / Returns', 'INCOME', 'trending_up', '#14B8A6', 1),
('cat_other_inc', NULL, 'Other Income', 'INCOME', 'attach_money', '#0D9488', 1);
