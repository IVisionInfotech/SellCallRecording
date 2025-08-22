package com.sellcallrecording.util

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Session @Inject constructor(private val sharedPreferences: SharedPreferences) {

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    fun putString(key: String?, value: String?) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getString(key: String?, defaultValues: String?): String? {
        return sharedPreferences.getString(key, defaultValues)
    }

    fun putInt(key: String?, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    fun getInt(key: String?, defaultValues: Int): Int {
        return sharedPreferences.getInt(key, defaultValues)
    }

    fun putLong(key: String?, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    fun getLong(key: String?, defaultValues: Long): Long {
        return sharedPreferences.getLong(key, defaultValues)
    }

    fun putBoolean(key: String?, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String?, defaultValues: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValues)
    }
}
