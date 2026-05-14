package com.reps.app.core.data.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())

    @TypeConverter
    fun fromLongList(value: List<Long>): String = json.encodeToString(value)

    @TypeConverter
    fun toLongList(value: String): List<Long> =
        runCatching { json.decodeFromString<List<Long>>(value) }.getOrDefault(emptyList())
}
