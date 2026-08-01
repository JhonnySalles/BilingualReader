package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceView

/**
 * SurfaceView customizada criada especificamente para o Filament ModelViewer.
 * O ModelViewer do Filament possui um bug nativo fatal (SIGSEGV) ao tentar limpar o carregamento assíncrono.
 * Ele tenta contornar isso chamando seu próprio destroy() sempre que a View sofre detach, o que piora tudo 
 * resultando em IllegalStateExceptions ou múltiplos destroy() no motor C++.
 * 
 * Esta classe resolve isso silenciosamente: quando o ModelViewer tentar registrar o seu OnAttachStateChangeListener,
 * nós o bloqueamos! Isso nos dá controle TOTAL e seguro sobre quando a Engine será limpa.
 */
class SafeSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr) {

    var ignoreListeners: Boolean = false

    override fun addOnAttachStateChangeListener(listener: OnAttachStateChangeListener) {
        if (ignoreListeners || listener.javaClass.name.contains("ModelViewer")) {
            // BLOQUEADO: Ignoramos silenciosamente o listener interno do ModelViewer
            return
        }
        super.addOnAttachStateChangeListener(listener)
    }
}
