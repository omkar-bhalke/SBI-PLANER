package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
  @PrimaryKey
  val id: String,
  val name: String,
  val type: TransactionType = TransactionType.EXPENSE,
  val iconName: String = "category",
  val colorHex: String = "#1E3E62",
  val isDefault: Boolean = false
) {
  companion object {
    fun getDefaultCategories(): List<CategoryEntity> = listOf(
      CategoryEntity("cat_food", "Food & Dining", TransactionType.EXPENSE, "restaurant", "#F59E0B", true),
      CategoryEntity("cat_transport", "Transportation", TransactionType.EXPENSE, "directions_bus", "#3B82F6", true),
      CategoryEntity("cat_education", "Education", TransactionType.EXPENSE, "school", "#8B5CF6", true),
      CategoryEntity("cat_printing", "Printing & Stationery", TransactionType.EXPENSE, "print", "#6366F1", true),
      CategoryEntity("cat_shopping", "Shopping", TransactionType.EXPENSE, "shopping_bag", "#EC4899", true),
      CategoryEntity("cat_entertainment", "Entertainment", TransactionType.EXPENSE, "movie", "#10B981", true),
      CategoryEntity("cat_recharge", "Mobile Recharge", TransactionType.EXPENSE, "phone_android", "#06B6D4", true),
      CategoryEntity("cat_personal", "Personal Care", TransactionType.EXPENSE, "person", "#14B8A6", true),
      CategoryEntity("cat_other_exp", "Other Expenses", TransactionType.EXPENSE, "more_horiz", "#64748B", true),

      // Income Categories
      CategoryEntity("cat_salary", "Salary / Allowance", TransactionType.INCOME, "account_balance_wallet", "#10B981", true),
      CategoryEntity("cat_scholarship", "Scholarship", TransactionType.INCOME, "card_giftcard", "#059669", true),
      CategoryEntity("cat_family", "Money from Family", TransactionType.INCOME, "family_restroom", "#047857", true),
      CategoryEntity("cat_investment", "Investments / Returns", TransactionType.INCOME, "trending_up", "#14B8A6", true),
      CategoryEntity("cat_other_inc", "Other Income", TransactionType.INCOME, "attach_money", "#0D9488", true)
    )
  }
}
