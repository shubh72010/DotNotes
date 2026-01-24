package com.folius.dotnotes.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromStringListToString(list: List<String>?): String {
        return Gson().toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun fromChecklistItemList(value: String?): List<ChecklistItem> {
        val listType = object : TypeToken<List<ChecklistItem>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromChecklistItemListToString(list: List<ChecklistItem>?): String {
        return Gson().toJson(list ?: emptyList<ChecklistItem>())
    }
}
