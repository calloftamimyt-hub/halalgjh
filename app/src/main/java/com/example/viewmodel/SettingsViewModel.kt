package com.example.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AppLanguage(val code: String, val label: String) {
    BENGALI("bn", "বাংলা"),
    ENGLISH("en", "English")
}

class SettingsViewModel(context: Context) : ViewModel() {
    private val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(
        AppLanguage.values().find { it.code == sharedPrefs.getString("language", "bn") } ?: AppLanguage.BENGALI
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    init {
        GlobalLanguage.isEnglish = _language.value == AppLanguage.ENGLISH
    }

    fun setLanguage(lang: AppLanguage) {
        _language.update { lang }
        GlobalLanguage.isEnglish = lang == AppLanguage.ENGLISH
        sharedPrefs.edit().putString("language", lang.code).apply()
    }
}
