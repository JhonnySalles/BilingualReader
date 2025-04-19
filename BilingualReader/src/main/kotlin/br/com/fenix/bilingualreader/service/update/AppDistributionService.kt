package br.com.fenix.bilingualreader.service.update

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface AppDistributionService {

    companion object {
        const val IdProject = "269550539712"
        const val IdApp = "1%3A269550539712%3Aandroid%3A11dc80ed33aaa4b14dee25"
    }


    @GET("v1/projects/$IdProject/apps/$IdApp/releases")
    fun getLastVersion(@Query("key") key: String, @Query("orderBy") create: String = "createTime desc", @Query("pageSize") size: Int = 1): Call<Releases>

}