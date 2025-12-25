package com.librefocus.data.local.database.converter

import androidx.room.TypeConverter
import com.librefocus.models.DayOfWeek
import com.librefocus.models.TimeSlot
import org.json.JSONArray
import org.json.JSONObject

/**
 * Room type converters for complex data types.
 */
object RoomTypeConverters {

    @TypeConverter
    @JvmStatic
    fun fromHourlyUnlockMap(map: Map<Int, Int>?): String {
        if (map == null) return "{}"
        val jsonObject = JSONObject()
        map.entries.sortedBy { it.key }.forEach { (hour, count) ->
            jsonObject.put(hour.toString(), count)
        }
        return jsonObject.toString()
    }

    @TypeConverter
    @JvmStatic
    fun toHourlyUnlockMap(data: String?): Map<Int, Int> {
        if (data.isNullOrBlank()) return emptyMap()
        val jsonObject = JSONObject(data)
        val result = mutableMapOf<Int, Int>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val hour = key.toIntOrNull() ?: continue
            result[hour] = jsonObject.optInt(key, 0)
        }
        return result.toSortedMap()
    }

    // Limit-related converters

    @TypeConverter
    @JvmStatic
    fun fromStringList(list: List<String>?): String {
        if (list == null || list.isEmpty()) return "[]"
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    @TypeConverter
    @JvmStatic
    fun toStringList(data: String?): List<String> {
        if (data.isNullOrBlank() || data == "[]") return emptyList()
        val result = mutableListOf<String>()
        val jsonArray = JSONArray(data)
        for (i in 0 until jsonArray.length()) {
            result.add(jsonArray.getString(i))
        }
        return result
    }

    @TypeConverter
    @JvmStatic
    fun fromTimeSlotList(list: List<TimeSlot>?): String {
        if (list == null || list.isEmpty()) return "[]"
        val jsonArray = JSONArray()
        list.forEach { slot ->
            val jsonObject = JSONObject()
            jsonObject.put("fromHour", slot.fromHour)
            jsonObject.put("toHour", slot.toHour)
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    @JvmStatic
    fun toTimeSlotList(data: String?): List<TimeSlot> {
        if (data.isNullOrBlank() || data == "[]") return emptyList()
        val result = mutableListOf<TimeSlot>()
        val jsonArray = JSONArray(data)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            result.add(
                TimeSlot(
                    fromHour = jsonObject.getInt("fromHour"),
                    toHour = jsonObject.getInt("toHour")
                )
            )
        }
        return result
    }

    @TypeConverter
    @JvmStatic
    fun fromDayOfWeekSet(set: Set<DayOfWeek>?): String {
        if (set == null || set.isEmpty()) return ""
        return set.joinToString(",") { it.name }
    }

    @TypeConverter
    @JvmStatic
    fun toDayOfWeekSet(data: String?): Set<DayOfWeek> {
        if (data.isNullOrBlank()) return emptySet()
        return data.split(",")
            .mapNotNull { 
                try {
                    DayOfWeek.valueOf(it.trim())
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            .toSet()
    }
}
