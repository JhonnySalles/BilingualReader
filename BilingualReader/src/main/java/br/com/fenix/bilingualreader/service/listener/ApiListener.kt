package br.com.fenix.bilingualreader.service.listener

interface ApiListener<T> {
    fun onSuccess(result: T)
    fun onFailure(message: String)
}