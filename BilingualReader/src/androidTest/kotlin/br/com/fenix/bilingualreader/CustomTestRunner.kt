package br.com.fenix.bilingualreader

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        return try {
            super.newApplication(cl, TestApplication::class.java.name, context)
        } catch (e: Throwable) {
            println("CustomTestRunner: Falha ao instanciar TestApplication, usando default. Erro: ${e.message}")
            super.newApplication(cl, className, context)
        }
    }
}
