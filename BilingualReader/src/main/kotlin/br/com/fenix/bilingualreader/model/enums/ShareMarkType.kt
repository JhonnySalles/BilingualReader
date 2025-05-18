package br.com.fenix.bilingualreader.model.enums

import android.content.Intent

enum class ShareMarkType {
    SUCCESS,
    SYNC_IN_PROGRESS,
    NOTIFY_DATA_SET,
    NOT_ALTERATION,

    NOT_SIGN_IN,
    NEED_PERMISSION_DRIVE,
    NOT_CONNECT_DRIVE,
    NOT_CONNECT_FIREBASE,

    ERROR_NETWORK,
    ERROR_DOWNLOAD,
    ERROR_UPLOAD,
    ERROR;

    var intent: Intent? = null

    companion object {
        var send: Int = 0
        var receive: Int = 0

        fun clear() {
            send = 0
            receive = 0
        }
    }
}