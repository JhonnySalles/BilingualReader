package br.com.fenix.bilingualreader.model.enums

import android.content.Intent

enum class ShareMarkType {
    SUCCESS,
    NOTIFY_DATA_SET,
    NOT_ALTERATION,

    NOT_SIGN_IN,
    NEED_PERMISSION_DRIVE,
    NOT_CONNECT_DRIVE,

    ERROR_NETWORK,
    ERROR_DOWNLOAD,
    ERROR_UPLOAD,
    ERROR;

    var intent: Intent? = null
}