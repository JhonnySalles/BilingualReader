package br.com.fenix.bilingualreader.service.tracker

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Information
import br.com.fenix.bilingualreader.service.tracker.mal.MalMangaDetail

class ParseInformation {
    companion object {
        fun <T> getInformation(context: Context, list: List<T>): MutableList<Information> {
            val newList = mutableListOf<Information>()
            for (item in list)
                newList.add(getInformation(context, item))

            newList.removeIf {
                it.title.isEmpty()
            }

            return newList
        }

        fun <T> getInformation(context: Context, item: T): Information {
            return when (item) {
                is MalMangaDetail -> Information(context, item as MalMangaDetail)
                else -> Information()
            }
        }

        fun getInformation(context: Context, item: MalMangaDetail): Information {
            return Information(context, item)
        }
    }
}