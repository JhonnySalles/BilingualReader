package br.com.fenix.bilingualreader

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.databinding.ActivityMainBinding
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.service.listener.MainListener
import br.com.fenix.bilingualreader.util.helpers.MenuUtil

/**
 * Atividade para hospedar fragmentos em testes de instrumentação,
 * utilizando o layout real de produção (activity_main) para garantir 
 * um ambiente idêntico ao aplicativo.
 */
class TestActivity : AppCompatActivity(), MainListener {

    private lateinit var mToolBar: Toolbar
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Usa o tema original do app para o teste
        val theme = Themes.ORIGINAL
        setTheme(theme.getValue())
        
        super.onCreate(savedInstanceState)
        
        // Infla o layout real de produção
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mToolBar = findViewById(R.id.main_toolbar)
        // Aplica o tint do toolbar igual na produção
        MenuUtil.tintToolbar(mToolBar, theme)
        setSupportActionBar(mToolBar)
    }

    /**
     * Insere o fragmento no container de produção R.id.main_content_root
     */
    fun setFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_content_root, fragment)
            .commitNow()
        
        // Força a atualização do menu de opções para que o Espresso o reconheça
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // A atividade de teste delega a criação do menu para os fragmentos
        return super.onCreateOptionsMenu(menu)
    }

    override fun showUpButton() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun hideUpButton() {
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun changeLibraryTitle(library: String) {
        mToolBar.title = library
    }

    override fun clearLibraryTitle() {
        mToolBar.title = getString(R.string.app_name)
    }
}
