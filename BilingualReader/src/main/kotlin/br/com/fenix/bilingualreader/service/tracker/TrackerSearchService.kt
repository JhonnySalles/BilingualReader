package br.com.fenix.bilingualreader.service.tracker

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Information
import br.com.fenix.bilingualreader.service.listener.ApiListener
import br.com.fenix.bilingualreader.service.tracker.anilist.AniListMedia
import br.com.fenix.bilingualreader.service.tracker.anilist.AniListTracker
import br.com.fenix.bilingualreader.service.tracker.mal.MalMangaDetail
import br.com.fenix.bilingualreader.service.tracker.mal.MyAnimeListTracker
import org.slf4j.LoggerFactory

class TrackerSearchService(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(TrackerSearchService::class.java)
    private val mMalTracker = MyAnimeListTracker(context)
    private val mAniListTracker = AniListTracker(context)

    fun searchMangaInformation(query: String, listener: ApiListener<List<Information>>) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            listener.onSuccess(emptyList())
            return
        }

        mLOGGER.info("Searching manga on MyAnimeList: $cleanQuery")
        mMalTracker.getListManga(cleanQuery, object : ApiListener<List<MalMangaDetail>> {
            override fun onSuccess(result: List<MalMangaDetail>) {
                val list = ParseInformation.getInformation(context, result)
                if (list.isNotEmpty()) {
                    mLOGGER.info("Found ${list.size} manga items on MyAnimeList")
                    listener.onSuccess(list)
                } else {
                    mLOGGER.info("No manga found on MyAnimeList, falling back to AniList: $cleanQuery")
                    searchAniListManga(cleanQuery, listener)
                }
            }

            override fun onFailure(message: String) {
                mLOGGER.warn("MyAnimeList manga search failed: $message. Falling back to AniList: $cleanQuery")
                searchAniListManga(cleanQuery, listener)
            }
        })
    }

    fun searchNovelInformation(query: String, listener: ApiListener<List<Information>>) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            listener.onSuccess(emptyList())
            return
        }

        mLOGGER.info("Searching novel on MyAnimeList: $cleanQuery")
        mMalTracker.getListNovel(cleanQuery, object : ApiListener<List<MalMangaDetail>> {
            override fun onSuccess(result: List<MalMangaDetail>) {
                val list = ParseInformation.getInformation(context, result)
                if (list.isNotEmpty()) {
                    mLOGGER.info("Found ${list.size} novel items on MyAnimeList")
                    listener.onSuccess(list)
                } else {
                    mLOGGER.info("No novel found on MyAnimeList, falling back to AniList: $cleanQuery")
                    searchAniListNovel(cleanQuery, listener)
                }
            }

            override fun onFailure(message: String) {
                mLOGGER.warn("MyAnimeList novel search failed: $message. Falling back to AniList: $cleanQuery")
                searchAniListNovel(cleanQuery, listener)
            }
        })
    }

    private fun searchAniListManga(query: String, listener: ApiListener<List<Information>>) {
        mAniListTracker.getMediaListManga(query, object : ApiListener<List<AniListMedia>> {
            override fun onSuccess(result: List<AniListMedia>) {
                val list = ParseInformation.getInformation(context, result)
                mLOGGER.info("Found ${list.size} manga items on AniList")
                listener.onSuccess(list)
            }

            override fun onFailure(message: String) {
                mLOGGER.warn("AniList manga search failed: $message")
                listener.onSuccess(emptyList())
            }
        })
    }

    private fun searchAniListNovel(query: String, listener: ApiListener<List<Information>>) {
        mAniListTracker.getMediaListNovel(query, object : ApiListener<List<AniListMedia>> {
            override fun onSuccess(result: List<AniListMedia>) {
                val list = ParseInformation.getInformation(context, result)
                mLOGGER.info("Found ${list.size} novel items on AniList")
                listener.onSuccess(list)
            }

            override fun onFailure(message: String) {
                mLOGGER.warn("AniList novel search failed: $message")
                listener.onSuccess(emptyList())
            }
        })
    }
}
