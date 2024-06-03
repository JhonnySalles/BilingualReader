package br.com.fenix.bilingualreader.service.listener

import android.view.View
import br.com.fenix.bilingualreader.model.entity.Tags

interface TagsListener {
    fun onCheckedChange(tag: Tags)
    fun onDelete(tag: Tags, view: View, position: Int)

    fun valid(name: String): Boolean
    fun save(tag: Tags)
}