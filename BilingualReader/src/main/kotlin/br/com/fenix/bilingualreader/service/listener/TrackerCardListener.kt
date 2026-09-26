package br.com.fenix.bilingualreader.service.listener

import br.com.fenix.bilingualreader.model.entity.Track

interface TrackerCardListener {
    fun onClick(track: Track)
    fun onLongClick(track: Track): Boolean = false
}
