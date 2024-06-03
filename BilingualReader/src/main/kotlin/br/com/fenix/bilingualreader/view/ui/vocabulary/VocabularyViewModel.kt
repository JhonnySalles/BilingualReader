package br.com.fenix.bilingualreader.view.ui.vocabulary

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.transformLatest
import org.slf4j.LoggerFactory


class VocabularyViewModel(application: Application) : AndroidViewModel(application) {

    private val mLOGGER = LoggerFactory.getLogger(VocabularyViewModel::class.java)

    private val mDataBase: VocabularyRepository =
        VocabularyRepository(application.applicationContext)

    private var mIsQuery = MutableLiveData(false)
    val isQuery: LiveData<Boolean> = mIsQuery

    private var mOrder = MutableLiveData(Pair(Order.Description, false))
    val order: LiveData<Pair<Order, Boolean>> = mOrder

    inner class Query(
        var vocabulary: String = "",
        var favorite: Boolean = false,
        var order: Pair<Order, Boolean> = Pair(Order.Description, false)
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

    fun setQuery(vocabulary: String) {
        setQuery(Query(vocabulary, currentQuery.value.favorite, currentQuery.value.order))
    }

    fun setQueryFavorite(favorite: Boolean) {
        setQuery(
            Query(
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
                currentQuery.value.vocabulary,
                currentQuery.value.favorite,
                sort
            )
        )
    }

    fun setQuery(vocabulary: String, favorite: Boolean) {
        setQuery(Query(vocabulary, favorite, currentQuery.value.order))
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
                val list = dao.list(query, page * params.loadSize, params.loadSize)

                //Simulation delay
                //if (page != 0) delay(10000)

                LoadResult.Page(
                    data = list,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = if (list.isEmpty()) null else page + 1
                )
            } catch (e: Exception) {
                mLOGGER.error("Error paging list vocabulary.", e)
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