package br.com.fenix.bilingualreader.util.helpers

import android.graphics.Bitmap
import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime

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


}