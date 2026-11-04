const { v4: uuidv4 } = require('uuid');
const db = require('../config/db');

exports.getCategories = async (req, res) => {
  try {
    const userId = req.user.userId;
    const [categories] = await db.query(
      'SELECT * FROM categories WHERE user_id = ? OR user_id IS NULL ORDER BY is_default DESC, category_name ASC',
      [userId]
    );
    return res.json({ success: true, categories });
  } catch (error) {
    console.error('Error fetching categories:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

exports.createCategory = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { categoryName, categoryType = 'EXPENSE', iconName = 'category', colorHex = '#1E3E62' } = req.body;

    if (!categoryName) {
      return res.status(400).json({ success: false, message: 'Category name is required' });
    }

    const id = uuidv4();
    await db.query(
      'INSERT INTO categories (id, user_id, category_name, category_type, icon_name, color_hex, is_default) VALUES (?, ?, ?, ?, ?, ?, false)',
      [id, userId, categoryName.trim(), categoryType, iconName, colorHex]
    );

    const [created] = await db.query('SELECT * FROM categories WHERE id = ?', [id]);
    return res.status(201).json({ success: true, category: created[0] });
  } catch (error) {
    console.error('Error creating category:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

exports.updateCategory = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { id } = req.params;
    const { categoryName, colorHex, iconName } = req.body;

    await db.query(
      'UPDATE categories SET category_name = COALESCE(?, category_name), color_hex = COALESCE(?, color_hex), icon_name = COALESCE(?, icon_name) WHERE id = ? AND user_id = ?',
      [categoryName, colorHex, iconName, id, userId]
    );

    const [updated] = await db.query('SELECT * FROM categories WHERE id = ?', [id]);
    return res.json({ success: true, category: updated[0] });
  } catch (error) {
    console.error('Error updating category:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};

exports.deleteCategory = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { id } = req.params;

    const [existing] = await db.query('SELECT * FROM categories WHERE id = ? AND user_id = ?', [id, userId]);
    if (existing.length === 0) {
      return res.status(404).json({ success: false, message: 'Category not found or default system category' });
    }

    await db.query('DELETE FROM categories WHERE id = ? AND user_id = ?', [id, userId]);
    return res.json({ success: true, message: 'Category deleted successfully' });
  } catch (error) {
    console.error('Error deleting category:', error);
    return res.status(500).json({ success: false, message: error.message });
  }
};
