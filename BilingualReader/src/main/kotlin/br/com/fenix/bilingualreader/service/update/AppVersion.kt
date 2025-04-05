package br.com.fenix.bilingualreader.service.update

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class Release(
    @SerializedName("name")
    val name: String,
    @SerializedName("releaseNotes")
    val note: ReleaseNote,
    @SerializedName("displayVersion")
    val version: String,
    @SerializedName("buildVersion")
    val build: String,
    @SerializedName("firebaseConsoleUri")
    val consoleUri: String,
    @SerializedName("testingUri")
    val testingUri: String,
    @SerializedName("binaryDownloadUri")
    val downloadUri: String,
    @SerializedName("createTime")
    val mean: LocalDateTime
)

data class ReleaseNote(
    @SerializedName("text")
    val note: String
)


data class Releases(
    @SerializedName("large")
    val large: List<Release>,
    @SerializedName("nextPageToken")
    val nextPage: String
)

