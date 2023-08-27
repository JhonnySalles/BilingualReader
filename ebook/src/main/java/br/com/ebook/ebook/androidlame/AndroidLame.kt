/*
 * Copyright (C) 2016 Naman Dwivedi
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package br.com.ebook.ebook.androidlame

import br.com.ebook.ebook.androidlame.LameBuilder.VbrMode

class AndroidLame(builder: LameBuilder) {
    init {
        initialize(builder)
    }

    private fun initialize(builder: LameBuilder) {
        initialize(
            builder.inSampleRate,
            builder.outChannel,
            builder.outSampleRate,
            builder.outBitrate,
            builder.scaleInput,
            getIntForMode(builder.mode),
            getIntForVbrMode(builder.vbrMode),
            builder.quality,
            builder.vbrQuality,
            builder.abrMeanBitrate,
            builder.lowpassFreq,
            builder.highpassFreq,
            builder.id3tagTitle,
            builder.id3tagArtist,
            builder.id3tagAlbum,
            builder.id3tagYear,
            builder.id3tagComment
        )
    }

    fun encode(
        buffer_l: ShortArray, buffer_r: ShortArray,
        samples: Int, mp3buf: ByteArray
    ): Int {
        return lameEncode(buffer_l, buffer_r, samples, mp3buf)
    }

    fun flush(mp3buf: ByteArray): Int {
        return lameFlush(mp3buf)
    }

    fun close() {
        lameClose()
    }

    companion object {
        init {
            System.loadLibrary("androidlame")
        }

        private external fun initialize(
            inSamplerate: Int, outChannel: Int,
            outSamplerate: Int, outBitrate: Int, scaleInput: Float, mode: Int, vbrMode: Int,
            quality: Int, vbrQuality: Int, abrMeanBitrate: Int, lowpassFreq: Int, highpassFreq: Int, id3tagTitle: String?,
            id3tagArtist: String?, id3tagAlbum: String?, id3tagYear: String?,
            id3tagComment: String?
        )

        private external fun lameEncode(
            buffer_l: ShortArray, buffer_r: ShortArray,
            samples: Int, mp3buf: ByteArray
        ): Int

        private external fun lameFlush(mp3buf: ByteArray): Int
        private external fun lameClose()

        ////UTILS
        private fun getIntForMode(mode: LameBuilder.Mode): Int {
            return when (mode) {
                LameBuilder.Mode.STEREO -> 0
                LameBuilder.Mode.JSTEREO -> 1
                LameBuilder.Mode.MONO -> 3
                LameBuilder.Mode.DEFAULT -> 4
            }
            return -1
        }

        private fun getIntForVbrMode(mode: VbrMode): Int {
            return when (mode) {
                VbrMode.VBR_OFF -> 0
                VbrMode.VBR_RH -> 2
                VbrMode.VBR_ABR -> 3
                VbrMode.VBR_MTRH -> 4
                VbrMode.VBR_DEFAUT -> 6
            }
            return -1
        }
    }
}