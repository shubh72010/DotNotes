package com.folius.dotnotes.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    companion object {
        // Cached Gson instance — avoids creating a new one per conversion call
        private val gson = Gson()
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromStringListToString(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun fromChecklistItemList(value: String?): List<ChecklistItem> {
        val listType = object : TypeToken<List<ChecklistItem>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromChecklistItemListToString(list: List<ChecklistItem>?): String {
        return gson.toJson(list ?: emptyList<ChecklistItem>())
    }

    @TypeConverter
    fun fromIntList(value: String?): List<Int> {
        val listType = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromIntListToString(list: List<Int>?): String {
        return gson.toJson(list ?: emptyList<Int>())
    }
    @TypeConverter
    fun fromMapItems(value: String?): List<MapItem> {
        val listType = object : TypeToken<List<MapItem>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun mapItemsToString(list: List<MapItem>?): String {
        return gson.toJson(list ?: emptyList<MapItem>())
    }
}
