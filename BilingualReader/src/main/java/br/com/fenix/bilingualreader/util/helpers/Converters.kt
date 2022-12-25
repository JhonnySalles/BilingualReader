package br.com.fenix.bilingualreader.util.helpers

import android.graphics.Bitmap
import androidx.room.TypeConverter
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

}