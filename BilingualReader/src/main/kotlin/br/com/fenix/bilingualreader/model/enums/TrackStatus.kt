package br.com.fenix.bilingualreader.model.enums

import com.google.gson.annotations.SerializedName

enum class TrackStatus(val description: String) {
    @SerializedName("reading")
    READING("Reading"),

    @SerializedName("completed")
    COMPLETED("Completed"),

    @SerializedName("on_hold")
    ON_HOLD("On Hold"),

    @SerializedName("dropped")
    DROPPED("Dropped"),

    @SerializedName("plan_to_read")
    PLAN_TO_READ("Plan to Read"),

    @SerializedName("rereading")
    REREADING("Rereading");

    companion object {
        fun fromString(value: String?): TrackStatus {
            if (value == null) return READING
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                entries.firstOrNull { it.name.equals(value, true) || it.description.equals(value, true) } ?: READING
            }
        }
    }
}
