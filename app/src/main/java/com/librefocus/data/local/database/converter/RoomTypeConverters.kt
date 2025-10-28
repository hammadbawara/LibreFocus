package com.librefocus.data.local.database.converter

import androidx.room.TypeConverter
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
}
