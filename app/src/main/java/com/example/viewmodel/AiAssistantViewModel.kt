package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TransactionEntity
import com.example.data.repository.FinancialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
  val id: String = java.util.UUID.randomUUID().toString(),
  val message: String,
  val isUser: Boolean,
  val timestamp: Long = System.currentTimeMillis()
)

class AiAssistantViewModel(application: Application) : AndroidViewModel(application) {
  val repository = FinancialRepository(application)

  private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

  private val _isAnalyzing = MutableStateFlow(false)
  val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

  val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  init {
    // Generate initial financial analysis greeting
    viewModelScope.launch {
      val txs = repository.allTransactions.first()
      val settings = repository.userSettings.first()
      val initialAnalysis = repository.aiAssistant.generateAnalysis(
        transactions = txs,
        dailyLimit = settings.dailySpendingLimit
      )
      _messages.value = listOf(
        ChatMessage(
          message = "Hello! I am your FinPulse AI Financial Assistant. I analyze your actual spending records, detect repeated costs, and help you stay on budget.\n\n$initialAnalysis",
          isUser = false
        )
      )
    }
  }

  fun sendQuestion(question: String) {
    if (question.isBlank()) return

    val userMsg = ChatMessage(message = question.trim(), isUser = true)
    _messages.value = _messages.value + userMsg

    viewModelScope.launch {
      _isAnalyzing.value = true
      val txs = repository.allTransactions.first()
      val settings = repository.userSettings.first()
      val response = repository.aiAssistant.generateAnalysis(
        transactions = txs,
        customPrompt = question.trim(),
        dailyLimit = settings.dailySpendingLimit
      )
      _messages.value = _messages.value + ChatMessage(message = response, isUser = false)
      _isAnalyzing.value = false
    }
  }

  fun refreshAnalysis() {
    viewModelScope.launch {
      _isAnalyzing.value = true
      val txs = repository.allTransactions.first()
      val settings = repository.userSettings.first()
      val analysis = repository.aiAssistant.generateAnalysis(
        transactions = txs,
        dailyLimit = settings.dailySpendingLimit
      )
      _messages.value = _messages.value + ChatMessage(
        message = "🔄 **Updated Financial Assessment**:\n\n$analysis",
        isUser = false
      )
      _isAnalyzing.value = false
    }
  }
}
