package br.com.fenix.bilingualreader.util.helpers

import android.graphics.Bitmap
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

class Converters {

    @TypeConverter
    fun fromBase64(image: String): Bitmap {
        return ImageUtil.decodeImageBase64(image)!!
    }

    @TypeConverter
    fun bitmapToBase64(image: Bitmap): String {
        return ImageUtil.encodeImageBase64(image)
    }

    @TypeConverter
    fun fromLocalDateTime(dateTime: String?): LocalDateTime? {
        if (dateTime == null)
            return null
        return LocalDateTime.parse(dateTime)
    }

    @TypeConverter
    fun localDateTimeToString(dateTime: LocalDateTime?): String? {
        if (dateTime == null)
            return null

        return dateTime.toString()
    }

    @TypeConverter
    fun fromLocalDate(date: String?): LocalDate? {
        if (date == null)
            return null
        return LocalDate.parse(date)
    }

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? {
        if (date == null)
            return null
        return date.toString()
    }

    @TypeConverter
    fun fromIntArray(array: String): IntArray {
        if (array.isEmpty())
            return intArrayOf()

        return array.split(",").map { it.toInt() }.toIntArray()
    }

    @TypeConverter
    fun intArrayToString(array: IntArray): String {
        if (array.isEmpty())
            return ""

        return array.joinToString(",")
    }

    @TypeConverter
    fun fromLongMutableList(array: String): MutableList<Long> {
        if (array.isEmpty())
            return mutableListOf()

        return array.split(",").map { it.toLong() }.toMutableList()
    }

    @TypeConverter
    fun longMutableListToString(array: MutableList<Long>): String {
        if (array.isEmpty())
            return ""

        return array.joinToString(",")
    }

    @TypeConverter
    fun fromIntMap(value: String): Map<Int, String> {
        if (value.isEmpty())
            return mapOf()

        val mapType = object : TypeToken<Map<Int, String>>() {}.type
        return Gson().fromJson(value, mapType)
    }

    @TypeConverter
    fun intMapToString(map: Map<Int, String>): String {
        if (map.isEmpty())
            return ""

        return Gson().toJson(map)
    }

    @TypeConverter
    fun dateToLong(date: Date?): Long? {
        if (date == null)
            return null
        return date.time
    }

    @TypeConverter
    fun fromDate(date: Long?): Date? {
        if (date == null)
            return null
        return Date(date)
    }

}