package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date


data class ShareAnnotation(
    @Expose
    @SerializedName(FIELD_PAGE)
    @PropertyName(FIELD_PAGE)
    @get:PropertyName(FIELD_PAGE)
    val page: Int,

    @Expose
    @SerializedName(FIELD_PAGES)
    @PropertyName(FIELD_PAGES)
    @get:PropertyName(FIELD_PAGES)
    var pages: Int,

    @Expose
    @SerializedName(FIELD_FONT_SIZE)
    @PropertyName(FIELD_FONT_SIZE)
    @get:PropertyName(FIELD_FONT_SIZE)
    var fontSize: Float,

    @Expose
    @SerializedName(FIELD_TYPE)
    @PropertyName(FIELD_TYPE)
    @get:PropertyName(FIELD_TYPE)
    val type: String,

    @Expose
    @SerializedName(FIELD_CHAPTER_NUMBER)
    @PropertyName(FIELD_CHAPTER_NUMBER)
    @get:PropertyName(FIELD_CHAPTER_NUMBER)
    var chapterNumber: Float,

    @Expose
    @SerializedName(FIELD_CHAPTER)
    @PropertyName(FIELD_CHAPTER)
    @get:PropertyName(FIELD_CHAPTER)
    val chapter: String,

    @Expose
    @SerializedName(FIELD_TEXT)
    @PropertyName(FIELD_TEXT)
    @get:PropertyName(FIELD_TEXT)
    var text: String,

    @Expose
    @SerializedName(FIELD_RANGE)
    @PropertyName(FIELD_RANGE)
    @get:PropertyName(FIELD_RANGE)
    var range: String,

    @Expose
    @SerializedName(FIELD_ANNOTATION)
    @PropertyName(FIELD_ANNOTATION)
    @get:PropertyName(FIELD_ANNOTATION)
    var annotation: String,

    @Expose
    @SerializedName(FIELD_FAVORITE)
    @PropertyName(FIELD_FAVORITE)
    @get:PropertyName(FIELD_FAVORITE)
    var favorite: Boolean,

    @Expose
    @SerializedName(FIELD_COLOR)
    @PropertyName(FIELD_COLOR)
    @get:PropertyName(FIELD_COLOR)
    var color: String,

    @Expose
    @SerializedName(FIELD_CREATED)
    @PropertyName(FIELD_CREATED)
    @get:PropertyName(FIELD_CREATED)
    var created: Date
) : Serializable {

    companion object {
        const val FIELD_PAGE = "page"
        const val FIELD_PAGES = "pages"
        const val FIELD_FONT_SIZE = "fontSize"
        const val FIELD_TYPE = "type"
        const val FIELD_CHAPTER_NUMBER = "chapterNumber"
        const val FIELD_CHAPTER = "chapter"
        const val FIELD_TEXT = "text"
        const val FIELD_RANGE = "range"
        const val FIELD_ANNOTATION = "annotation"
        const val FIELD_FAVORITE = "favorite"
        const val FIELD_COLOR = "color"
        const val FIELD_CREATED = "created"

        private fun getFloat(field: Any?, default: Float): Float {
            return try {
                (field as Double).toFloat()
            } catch (e: Exception) {
                try {
                    (field as Float)
                } catch (e: Exception) {
                    try {
                        (field as Long).toFloat()
                    } catch (e: Exception) {
                        default
                    }
                }
            }
        }
    }

    constructor(firebase: Map<String, *>) : this(
        (firebase[FIELD_PAGE] as Long).toInt(), (firebase[FIELD_PAGES] as Long).toInt(), getFloat(firebase[FIELD_FONT_SIZE], GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT), firebase[FIELD_TYPE] as String,
        getFloat(firebase[FIELD_CHAPTER_NUMBER], 0f), firebase[FIELD_CHAPTER] as String, firebase[FIELD_TEXT] as String,
        firebase[FIELD_RANGE] as String, firebase[FIELD_ANNOTATION] as String, firebase[FIELD_FAVORITE] as Boolean,  firebase[FIELD_COLOR] as String,
        (firebase[FIELD_CREATED] as Timestamp).toDate(),
    )

    constructor(annotation: BookAnnotation) : this(
        annotation.page, annotation.pages, annotation.fontSize, annotation.type.toString(), annotation.chapterNumber, annotation.chapter, annotation.text,
        Util.intArrayToString(annotation.range), annotation.annotation, annotation.favorite, annotation.color.toString(), GeneralConsts.dateTimeToDate(annotation.created)
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShareAnnotation

        if (page != other.page) return false
        if (pages != other.pages) return false
        if (fontSize != other.fontSize) return false
        if (type != other.type) return false
        if (range != other.range) return false
        if (color != other.color) return false

        return true
    }

    override fun hashCode(): Int {
        var result = page
        result = 31 * result + pages
        result = 31 * result + fontSize.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + range.hashCode()
        result = 31 * result + color.hashCode()
        return result
    }

    override fun toString(): String {
        return "ShareAnnotation(page=$page, pages=$pages, fontSize=$fontSize, type='$type', chapterNumber=$chapterNumber, chapter='$chapter', text='$text', range='$range', annotation='$annotation', favorite=$favorite, color='$color', created=$created)"
    }

}