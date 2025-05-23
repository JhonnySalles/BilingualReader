package br.com.fenix.bilingualreader.view.ui.vocabulary.manga

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.cachedIn
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyActivity
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.transformLatest
import org.slf4j.LoggerFactory


class VocabularyMangaViewModel(application: Application) : AndroidViewModel(application) {

    private val mLOGGER = LoggerFactory.getLogger(VocabularyMangaViewModel::class.java)

    private val mDataBase: VocabularyRepository =
        VocabularyRepository(application.applicationContext)

    private var mIsQuery = MutableLiveData(false)
    val isQuery: LiveData<Boolean> = mIsQuery

    private var mOrder = MutableLiveData(Pair(VocabularyActivity.mSortType, VocabularyActivity.mSortDesc))
    val order: LiveData<Pair<Order, Boolean>> = mOrder

    inner class Query(
        var manga: String = "",
        var vocabulary: String = VocabularyActivity.mVocabularySelect,
        var favorite: Boolean = VocabularyActivity.mIsFavorite,
        var order: Pair<Order, Boolean> = Pair(VocabularyActivity.mSortType, VocabularyActivity.mSortDesc)
    )

    private val currentQuery = MutableStateFlow(Query())

    fun vocabularyPager() =
        currentQuery.transformLatest { query ->
            Pager(PagingConfig(pageSize = 40)) {
                PagingSource(mDataBase, query)
            }.flow.collectLatest {
                emit(it)
                mIsQuery.value = false
            }
        }.cachedIn(viewModelScope)

    private fun setQuery(query: Query) {
        mIsQuery.value = true
        currentQuery.value = query
    }

    fun setQuery(manga: String, vocabulary: String) {
        setQuery(
            Query(
                manga,
                vocabulary,
                currentQuery.value.favorite,
                currentQuery.value.order
            )
        )
    }

    fun setQueryFavorite(favorite: Boolean) {
        setQuery(
            Query(
                currentQuery.value.manga,
                currentQuery.value.vocabulary,
                favorite,
                currentQuery.value.order
            )
        )
    }

    fun setQueryOrder(orderInverse: Boolean) {
        val sort = Pair(mOrder.value!!.first, orderInverse)
        mOrder.value = sort

        setQuery(
            Query(
                currentQuery.value.manga,
                currentQuery.value.vocabulary,
                currentQuery.value.favorite,
                sort
            )
        )
    }

    fun setQuery(manga: String, vocabulary: String, favorite: Boolean) {
        setQuery(Query(manga, vocabulary, favorite))
    }

    fun clearQuery() {
        setQuery(Query())
    }

    fun getFavorite(): Boolean = currentQuery.value.favorite

    fun getOrder(): Pair<Order, Boolean> = currentQuery.value.order

    fun update(vocabulary: Vocabulary) {
        mDataBase.update(vocabulary)
    }

    fun sorted(order: Order, isDesc: Boolean = false) {
        val sort = Pair(order, isDesc)
        mOrder.value = sort

        setQuery(
            Query(
                currentQuery.value.manga,
                currentQuery.value.vocabulary,
                currentQuery.value.favorite,
                sort
            )
        )
    }

    inner class PagingSource(private val dao: VocabularyRepository, private val query: Query) :
        androidx.paging.PagingSource<Int, Vocabulary>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Vocabulary> {
            val page = params.key ?: 0
            return try {
                val list = dao.listManga(query, page * params.loadSize, params.loadSize)

                for (vocabulary in list)
                    dao.findVocabMangaByVocabulary(query.manga, vocabulary)

                //Simulation delay
                //if (page != 0) delay(10000)

                LoadResult.Page(
                    data = list,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = if (list.isEmpty()) null else page + 1
                )
            } catch (e: Exception) {
                mLOGGER.error("Error paging list vocabulary: " + e.message, e)
                Firebase.crashlytics.apply {
                    setCustomKey("message", "Error paging list vocabulary: " + e.message)
                    recordException(e)
                }
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, Vocabulary>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                val anchorPage = state.closestPageToPosition(anchorPosition)
                anchorPage?.prevKey ?: anchorPage?.nextKey
            }
        }
    }

}