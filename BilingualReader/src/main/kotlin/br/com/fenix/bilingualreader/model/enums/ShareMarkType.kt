package br.com.fenix.bilingualreader.model.enums

import android.content.Intent

enum class ShareMarkType {
    SUCCESS,
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
        var send: Boolean = false
        var receive: Boolean = false

        fun clear() {
            send = false
            receive = false
        }
    }
}