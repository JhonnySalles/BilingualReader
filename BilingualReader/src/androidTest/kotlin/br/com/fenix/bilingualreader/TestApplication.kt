package br.com.fenix.bilingualreader

import android.app.Application
import androidx.room.Room
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import com.google.firebase.FirebaseApp
import io.sentry.android.core.SentryAndroid

class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Silenciar Telemetria antes de qualquer coisa
        Telemetry.isEnabled = false

        try {
            // 2. Configurar Banco de Dados em Memória para Testes
            // Isso evita overhead de IO e leitura de assets pesados que causam timeout no startup
            val db = Room.inMemoryDatabaseBuilder(this, DataBase::class.java)
                .allowMainThreadQueries() // Necessário para Espresso Tests
                .build()
            DataBase.setTestingInstance(db)
            DataBase.mAssets = this.assets // Inicializando assets para evitar lateinit crash

            // 3. Silenciando Firebase e Sentry (caso os providers ainda consigam rodar)
            FirebaseApp.getApps(this).forEach { it.setAutomaticResourceManagementEnabled(false) }
            
            SentryAndroid.init(this) { options ->
                options.dsn = ""
                options.isEnabled = false
            }
        } catch (e: Throwable) {
            println("TestApplication: Erro na inicialização de teste: ${e.message}")
        }
    }
}
