package br.com.fenix.bilingualreader.util.helpers

import android.graphics.Bitmap
import androidx.room.TypeConverter
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Converters {

    @TypeConverter
    fun fromBase64(image: String): Bitmap {
        return ImageUtil.decodeImageBase64(image)
    }

    @TypeConverter
    fun bitmapToBase64(image: Bitmap): String {
        return ImageUtil.encodeImageBase64(image)
    }

    @TypeConverter
    fun fromLocalDateTime(dateTime: String?): Date? {
        if (dateTime == null)
            return null

        return SimpleDateFormat(GeneralConsts.PATTERNS.DATE_TIME_PATTERN, Locale.getDefault()).parse(dateTime)
    }

    @TypeConverter
    fun localDateTimeToString(dateTime: Date?): String? {
        if (dateTime == null)
            return null

        return SimpleDateFormat(GeneralConsts.PATTERNS.DATE_TIME_PATTERN, Locale.getDefault()).format(dateTime)
    }

    /* @TypeConverter
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
    }*/

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

    /*@TypeConverter
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
    }*/

}