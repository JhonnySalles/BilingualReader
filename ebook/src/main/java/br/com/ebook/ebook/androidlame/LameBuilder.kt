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

class LameBuilder {
    enum class Mode {
        STEREO, JSTEREO, MONO, DEFAULT //DUAL_CHANNEL not supported
    }

    enum class VbrMode {
        VBR_OFF, VBR_RH, VBR_MTRH, VBR_ABR, VBR_DEFAUT
    }

    @JvmField
    var inSampleRate = 44100
    @JvmField
    var outSampleRate = 0
    @JvmField
    var outBitrate = 128
    @JvmField
    var outChannel = 2
    @JvmField
    var quality = 5
    @JvmField
    var vbrQuality: Int
    @JvmField
    var abrMeanBitrate: Int
    @JvmField
    var lowpassFreq: Int
    @JvmField
    var highpassFreq: Int
    @JvmField
    var scaleInput = 1f
    @JvmField
    var mode: Mode
    @JvmField
    var vbrMode: VbrMode
    @JvmField
    var id3tagTitle: String? = null
    @JvmField
    var id3tagArtist: String? = null
    @JvmField
    var id3tagAlbum: String? = null
    @JvmField
    var id3tagComment: String? = null
    @JvmField
    var id3tagYear: String? = null

    init {

        //default 0, Lame picks best according to compression
        mode = Mode.DEFAULT
        vbrMode = VbrMode.VBR_OFF
        vbrQuality = 5
        abrMeanBitrate = 128

        //default =0, Lame chooses
        lowpassFreq = 0
        highpassFreq = 0
    }

    fun setQuality(quality: Int): LameBuilder {
        this.quality = quality
        return this
    }

    fun setInSampleRate(inSampleRate: Int): LameBuilder {
        this.inSampleRate = inSampleRate
        return this
    }

    fun setOutSampleRate(outSampleRate: Int): LameBuilder {
        this.outSampleRate = outSampleRate
        return this
    }

    fun setOutBitrate(bitrate: Int): LameBuilder {
        outBitrate = bitrate
        return this
    }

    fun setOutChannels(channels: Int): LameBuilder {
        outChannel = channels
        return this
    }

    fun setId3tagTitle(title: String?): LameBuilder {
        id3tagTitle = title
        return this
    }

    fun setId3tagArtist(artist: String?): LameBuilder {
        id3tagArtist = artist
        return this
    }

    fun setId3tagAlbum(album: String?): LameBuilder {
        id3tagAlbum = album
        return this
    }

    fun setId3tagComment(comment: String?): LameBuilder {
        id3tagComment = comment
        return this
    }

    fun setId3tagYear(year: String?): LameBuilder {
        id3tagYear = year
        return this
    }

    fun setScaleInput(scaleAmount: Float): LameBuilder {
        scaleInput = scaleAmount
        return this
    }

    fun setMode(mode: Mode): LameBuilder {
        this.mode = mode
        return this
    }

    fun setVbrMode(mode: VbrMode): LameBuilder {
        vbrMode = mode
        return this
    }

    fun setVbrQuality(quality: Int): LameBuilder {
        vbrQuality = quality
        return this
    }

    fun setAbrMeanBitrate(bitrate: Int): LameBuilder {
        abrMeanBitrate = bitrate
        return this
    }

    fun setLowpassFreqency(freq: Int): LameBuilder {
        lowpassFreq = freq
        return this
    }

    fun setHighpassFreqency(freq: Int): LameBuilder {
        highpassFreq = freq
        return this
    }

    fun build(): AndroidLame {
        return AndroidLame(this)
    }
}