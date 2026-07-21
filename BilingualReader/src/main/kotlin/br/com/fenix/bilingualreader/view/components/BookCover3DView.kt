package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.view.Choreographer
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import br.com.fenix.bilingualreader.view.ui.detail.manga.MangaDetailFragment
import com.google.android.filament.*
import com.google.android.filament.gltfio.*
import com.google.android.filament.utils.*
import com.google.android.filament.android.UiHelper
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Visualizador 3D de Livro/Mangá usando Filament.
 * Renderiza um modelo 3D GLB e aplica a imagem de capa completa como textura.
 * Possui suporte a rotação por toque na tela (swipe) e fundo transparente.
 */
class BookCover3DView(
    private val context: Context,
    private val surfaceView: SurfaceView,
    private val isPopup: Boolean = false
) : SurfaceHolder.Callback {

    private val mLOGGER = LoggerFactory.getLogger(BookCover3DView::class.java)

    companion object {
        init {
            // Inicializa a biblioteca nativa do Filament
            com.google.android.filament.utils.Utils.init()
        }

        // =========================================================================
        // CONSTANTES PARA AJUSTE MANUAL DAS TEXTURAS E UVs
        // Modifique estes coeficientes de acordo com a imagem real "Volume 00 Tudo"
        // =========================================================================

        // Proporção de largura da capa da frente (lado esquerdo na textura)
        const val FRONT_COVER_WIDTH_RATIO = 0.475f

        // Proporção de largura da lombada (região central na textura)
        const val SPINE_WIDTH_RATIO = 0.05f

        // Proporção de largura da capa de trás (lado direito na textura)
        const val BACK_COVER_WIDTH_RATIO = 0.475f
    }

    private var modelViewer: ModelViewer? = null
    private val choreographer = Choreographer.getInstance()
    private val frameScheduler = FrameCallback()
    private var isSurfaceAvailable = false
    private var pendingBitmap: Bitmap? = null
    private var pendingIsFullCover = false
    private var pendingOnReady: (() -> Unit)? = null
    private var isDestroyed = false
    
    private var backLightEntity: Int = 0
    private val forwardVector = FloatArray(3)
    private val upVector = FloatArray(3)

    var onLongClickListener: (() -> Unit)? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            onLongClickListener?.invoke() ?: surfaceView.performLongClick()
        }
    })

    init {
        // Configura a SurfaceView para suportar fundo transparente
        surfaceView.holder.addCallback(this)
        surfaceView.setZOrderOnTop(true)
        surfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
        
        // Configura o interceptor de toques na SurfaceView para bloquear a rolagem do scroll pai
        surfaceView.setOnTouchListener { _, event ->
            onTouchEvent(event)
        }
        
        // Adiciona um listener seguro de detach para limpar os recursos quando a Activity for destruída,
        // garantindo que não vamos depender da limpeza falha padrão do ModelViewer.
        surfaceView.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: android.view.View) {}
            override fun onViewDetachedFromWindow(v: android.view.View) {
                cleanup()
            }
        })
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        isDestroyed = false // Reset the flag so the view can be reused when surface is recreated

        // O ModelViewer do Filament cria e gerencia internamente View, Scene, Camera, Renderer e SwapChain
        // Usamos UiHelper configurado para transparente (isOpaque = false)
        val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
            isOpaque = false
        }
        val viewer = ModelViewer(surfaceView, uiHelper = uiHelper).also { this.modelViewer = it }
        
        // Agora com o SafeSurfaceView, o listener destrutivo do Filament é ignorado nativamente.
        // O ciclo de vida fica totalmente sob nosso controle!

        // Configuração do Renderer para garantir a transparência
        val clearOptions = viewer.renderer.clearOptions
        clearOptions.clear = true
        viewer.renderer.clearOptions = clearOptions

        // Configuração da View para blend transparente
        viewer.view.apply {
            blendMode = View.BlendMode.TRANSLUCENT
            // Desativa a iluminação padrão do céu que obstrui o fundo transparente
            ambientOcclusionOptions = ambientOcclusionOptions.apply {
                enabled = false
            }
        }

        // Limpa o Skybox para não cobrir o fundo
        viewer.scene.skybox = null

        val lm = viewer.engine.lightManager
        
        // Ajusta a luz solar padrão para iluminar a Frente. A direção exata será recalculada a cada frame acompanhando a câmera.
        val lightInstance = lm.getInstance(viewer.light)
        if (lightInstance != 0) {
            lm.setIntensity(lightInstance, 120000.0f)
        }

        // Cria uma segunda luz direcional focada na parte de Trás do livro, que também acompanhará a câmera dinamicamente.
        backLightEntity = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 1.0f, 1.0f)
            .intensity(100000.0f)
            .build(viewer.engine, backLightEntity)
        viewer.scene.addEntity(backLightEntity)

        isSurfaceAvailable = true
        loadModel()
        setupCamera()
        choreographer.postFrameCallback(frameScheduler)

        // Se havia uma textura pendente aguardando a criação do surface, aplica agora
        pendingBitmap?.let {
            setBookTexture(it, pendingIsFullCover, pendingOnReady)
            pendingBitmap = null
            pendingOnReady = null
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // O ModelViewer do Filament se encarrega do redimensionamento do Viewport
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceAvailable = false
        choreographer.removeFrameCallback(frameScheduler)
        cleanup()
    }

    private fun loadModel() {
        val assetManager = context.assets
        try {
            val inputStream = assetManager.open("models/3d_book_cover.glb")
            val bytes = inputStream.readBytes()
            val byteBuffer = ByteBuffer.allocateDirect(bytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(bytes)
                flip()
            }
            val viewer = modelViewer ?: return
            
            // Ao invés de viewer.loadModelGlb(byteBuffer) (que inicia um carregamento assíncrono que crasha no destroy)
            // fazemos o equivalente usando carregamento síncrono.
            
            val fAssetLoader = viewer.javaClass.getDeclaredField("assetLoader")
            fAssetLoader.isAccessible = true
            val assetLoader = fAssetLoader.get(viewer) as AssetLoader
            
            val asset = assetLoader.createAsset(byteBuffer)
            
            val fAsset = viewer.javaClass.getDeclaredField("asset")
            fAsset.isAccessible = true
            fAsset.set(viewer, asset)
            
            if (asset != null) {
                val fResourceLoader = viewer.javaClass.getDeclaredField("resourceLoader")
                fResourceLoader.isAccessible = true
                val resourceLoader = fResourceLoader.get(viewer) as ResourceLoader
                
                resourceLoader.loadResources(asset) // Carregamento Síncrono!
                
                val fAnimator = viewer.javaClass.getDeclaredField("animator")
                fAnimator.isAccessible = true
                fAnimator.set(viewer, asset.instance.animator)
                
                asset.releaseSourceData()
            }
            
            viewer.transformToUnitCube()
            
            // Em vez de sobrescrever com uma matriz de escala pura que reseta a translação calculada pelo transformToUnitCube,
            // nós lemos a matriz atual, multiplicamos seus fatores de escala e definimos novamente.
            val tm = viewer.engine.transformManager
            val rootEntity = viewer.asset?.root ?: 0
            if (rootEntity != 0) {
                val instance = tm.getInstance(rootEntity)
                if (instance != 0) {
                    val currentTransform = FloatArray(16)
                    tm.getTransform(instance, currentTransform)
                    
                    // Rotaciona o livro em 180 graus (Math.PI radianos) no eixo Y para mostrar a frente da capa para a câmera
                    val cos180 = -1.0f
                    val sin180 = 0.0f
                    
                    // Multiplica a matriz de transformToUnitCube por uma matriz de rotação em Y de 180 graus e escala
                    // Matriz de rotação Y combinada com escala:
                    // [  cos(180)*S,   0,   sin(180)*S,   0 ]
                    // [           0,   S,            0,   0 ]
                    // [ -sin(180)*S,   0,   cos(180)*S,   0 ]
                    // [           0,   0,            0,   1 ]
                    val s = if (isPopup) 1.5f else 1.9f
                    
                    val r00 = cos180 * s
                    val r02 = sin180 * s
                    val r11 = s
                    val r20 = -sin180 * s
                    val r22 = cos180 * s
                    
                    // Multiplicação de matrizes para preservar a translação (posição centralizada)
                    val m00 = currentTransform[0] * r00 + currentTransform[8] * r20
                    val m01 = currentTransform[1] * r00 + currentTransform[9] * r20
                    val m02 = currentTransform[2] * r00 + currentTransform[10] * r20
                    val m03 = currentTransform[3] * r00 + currentTransform[11] * r20
                    
                    val m10 = currentTransform[4] * r11
                    val m11 = currentTransform[5] * r11
                    val m12 = currentTransform[6] * r11
                    val m13 = currentTransform[7] * r11
                    
                    val m20 = currentTransform[0] * r02 + currentTransform[8] * r22
                    val m21 = currentTransform[1] * r02 + currentTransform[9] * r22
                    val m22 = currentTransform[2] * r02 + currentTransform[10] * r22
                    val m23 = currentTransform[3] * r02 + currentTransform[11] * r22
                    
                    currentTransform[0] = m00
                    currentTransform[1] = m01
                    currentTransform[2] = m02
                    currentTransform[3] = m03
                    
                    currentTransform[4] = m10
                    currentTransform[5] = m11
                    currentTransform[6] = m12
                    currentTransform[7] = m13
                    
                    currentTransform[8] = m20
                    currentTransform[9] = m21
                    currentTransform[10] = m22
                    currentTransform[11] = m23
                    
                    // Desloca o livro para baixo no viewport (Y negativo na matriz column-major, índice 13)
                    currentTransform[13] = currentTransform[13] - (if (isPopup) 0.5f else 0.9f)

                    tm.setTransform(instance, currentTransform)
                }
            }
            
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isColorSimilar(color1: Int, color2: Int): Boolean {
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF
        
        val dr = r1 - r2
        val dg = g1 - g2
        val db = b1 - b2
        val distanceSquared = dr * dr + dg * dg + db * db
        return distanceSquared <= 1500
    }

    private fun calculateFlaps(bitmap: Bitmap): Pair<Int, Int> {
        val width = bitmap.width
        val height = bitmap.height
        val centerY = height / 2

        var leftCrop = 0
        val leftColor = bitmap.getPixel(0, centerY)
        for (x in 0 until width / 3) {
            val color = bitmap.getPixel(x, centerY)
            if (!isColorSimilar(leftColor, color)) {
                leftCrop = x
                break
            }
        }

        var rightCrop = 0
        val rightColor = bitmap.getPixel(width - 1, centerY)
        for (x in width - 1 downTo width * 2 / 3) {
            val color = bitmap.getPixel(x, centerY)
            if (!isColorSimilar(rightColor, color)) {
                rightCrop = (width - 1) - x
                break
            }
        }

        val effectiveWidth = width - leftCrop - rightCrop
        val ratio = effectiveWidth.toFloat() / height.toFloat()
        
        // Verifica se a detecção por cor trouxe um corte com proporção plausível (~1.35 a 1.65)
        if (leftCrop > 0 || rightCrop > 0) {
            if (ratio in 1.35f..1.65f) {
                return Pair(leftCrop, rightCrop)
            }
        }

        // Fallback: Se a imagem é excessivamente larga (ex: > 1.7), força a proporção 3:2 (1.5)
        val originalRatio = width.toFloat() / height.toFloat()
        if (originalRatio > 1.7f) {
            val expectedWidth = (height * 1.5f).toInt()
            val excess = width - expectedWidth
            if (excess > 0) {
                return Pair(excess / 2, excess - (excess / 2))
            }
        }

        return Pair(0, 0)
    }

    /**
     * Extrai de forma rápida e eficiente a cor predominante (média) do Bitmap.
     */
    private fun getPredominantColor(bitmap: Bitmap): Int {
        var redSum = 0L
        var greenSum = 0L
        var blueSum = 0L
        val width = bitmap.width
        val height = bitmap.height
        val stepX = (width / 20).coerceAtLeast(1)
        val stepY = (height / 20).coerceAtLeast(1)
        var count = 0
        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val color = bitmap.getPixel(x, y)
                redSum += (color shr 16) and 0xFF
                greenSum += (color shr 8) and 0xFF
                blueSum += color and 0xFF
                count++
            }
        }
        val r = (redSum / count).toInt()
        val g = (greenSum / count).toInt()
        val b = (blueSum / count).toInt()
        return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
    }

    fun setBookTexture(bitmap: Bitmap, isFullCover: Boolean = false, onReady: (() -> Unit)? = null) {
        if (isDestroyed || !isSurfaceAvailable) {
            // Se o motor foi destruído ou a surface não está pronta, salva na fila
            pendingBitmap = bitmap
            pendingIsFullCover = isFullCover
            pendingOnReady = onReady
            return
        }

        val viewer = modelViewer ?: return
        val engine = viewer.engine

        var finalBitmap = bitmap
        val assetManager = context.assets

        try {
            // 1. Carrega a malha base do asset
            val meshInputStream = assetManager.open("models/malha_book_cover.png")
            val rawMeshBitmap = android.graphics.BitmapFactory.decodeStream(meshInputStream)
            meshInputStream.close()

            if (rawMeshBitmap != null) {
                val meshWidth = rawMeshBitmap.width
                val meshHeight = rawMeshBitmap.height

                // Cria o bitmap mutável em que vamos pintar
                val combinedBitmap = Bitmap.createBitmap(meshWidth, meshHeight, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(combinedBitmap)

                // 2. Determina a cor padrão para preencher a malha
                val baseColor = getPredominantColor(bitmap)

                // 3. Processa a malha base em lote para trocar o verde limão (0xFF3CFF00 ou similar) pela cor predominante
                val pixels = IntArray(meshWidth * meshHeight)
                rawMeshBitmap.getPixels(pixels, 0, meshWidth, 0, 0, meshWidth, meshHeight)
                rawMeshBitmap.recycle()

                // Substitui a cor verde limão (valores próximos a #3CFF00) pela cor predominante da capa
                // Tolerância leve para transições de verde limão na borda da malha
                for (i in pixels.indices) {
                    val pixel = pixels[i]
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    
                    // Condição rápida para detectar o verde limão #3CFF00
                    if (g > 200 && r < 100 && b < 50) {
                        pixels[i] = baseColor
                    }
                }
                combinedBitmap.setPixels(pixels, 0, meshWidth, 0, 0, meshWidth, meshHeight)

                // 4. Recorta e pinta cada parte da capa original nas posições especificadas na malha
                // Larguras e alturas para corte a partir do bitmap original
                val originalHeight = bitmap.height

                if (isFullCover) {
                    // Calcula o tamanho das abas (marcadores) caso a imagem seja mais larga que o padrão
                    val (cropLeft, cropRight) = calculateFlaps(bitmap)
                    val effectiveWidth = bitmap.width - cropLeft - cropRight

                    // Frações da capa
                    val frontWidth = (effectiveWidth * FRONT_COVER_WIDTH_RATIO).toInt()
                    val spineWidth = (effectiveWidth * SPINE_WIDTH_RATIO).toInt()

                    // Recortes na capa original (esquerda = Frente, centro = Lombada, direita = Trás)
                    val frontSrc = android.graphics.Rect(cropLeft, 0, cropLeft + frontWidth, originalHeight)
                    val spineSrc = android.graphics.Rect(cropLeft + frontWidth, 0, cropLeft + frontWidth + spineWidth, originalHeight)
                    val backSrc = android.graphics.Rect(cropLeft + frontWidth + spineWidth, 0, bitmap.width - cropRight, originalHeight)

                    // Tras: Left=0, Top=1250, Right=1750, Bottom=4096. Rotacionado 180°
                    val backDst = android.graphics.RectF(0f, 1250f, 1750f, 4096f)
                    canvas.save()
                    canvas.rotate(180f, backDst.centerX(), backDst.centerY())
                    canvas.drawBitmap(bitmap, backSrc, backDst, null)
                    canvas.restore()

                    // Frente: Left=1758, Top=1250, Right=3520, Bottom=4096. Rotacionado 180°
                    val frontDst = android.graphics.RectF(1758f, 1250f, 3520f, 4096f)
                    canvas.save()
                    canvas.rotate(180f, frontDst.centerX(), frontDst.centerY())
                    canvas.drawBitmap(bitmap, frontSrc, frontDst, null)
                    canvas.restore()

                    // Lombada: Left=0, Top=276, Right=2880, Bottom=770.
                    // Rotacionamos a lombada -90 graus (sentido anti-horário)
                    val spineDst = android.graphics.RectF(0f, 276f, 2880f, 770f)
                    canvas.save()
                    canvas.rotate(-90f, spineDst.centerX(), spineDst.centerY())
                    // Ajusta proporção no desenho rotacionado
                    val spineRotatedDst = android.graphics.RectF(
                        spineDst.centerX() - (spineDst.height() / 2f),
                        spineDst.centerY() - (spineDst.width() / 2f),
                        spineDst.centerX() + (spineDst.height() / 2f),
                        spineDst.centerY() + (spineDst.width() / 2f)
                    )
                    canvas.drawBitmap(bitmap, spineSrc, spineRotatedDst, null)
                    canvas.restore()
                } else {
                    val originalWidth = bitmap.width
                    val frontSrc = android.graphics.Rect(0, 0, originalWidth, originalHeight)
                    val frontDst = android.graphics.RectF(1758f, 1250f, 3520f, 4096f)
                    canvas.save()
                    canvas.rotate(180f, frontDst.centerX(), frontDst.centerY())
                    canvas.drawBitmap(bitmap, frontSrc, frontDst, null)
                    canvas.restore()
                }

                finalBitmap = combinedBitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Cria a textura no Filament
        val texture = Texture.Builder()
            .width(finalBitmap.width)
            .height(finalBitmap.height)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(Texture.InternalFormat.SRGB8_A8)
            .build(engine)

        // Carrega o bitmap na textura
        val buffer = ByteBuffer.allocateDirect(finalBitmap.byteCount).apply {
            order(ByteOrder.nativeOrder())
        }
        finalBitmap.copyPixelsToBuffer(buffer)
        buffer.flip()

        texture.setImage(
            engine,
            0,
            Texture.PixelBufferDescriptor(
                buffer,
                Texture.Format.RGBA,
                Texture.Type.UBYTE
            )
        )

        // Salva a imagem no cache
        /*try {
            val cacheFile = java.io.File(context.cacheDir, "debug_combined_book_cover.png")
            java.io.FileOutputStream(cacheFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            android.util.Log.d("BookCover3DView", "Saved debug texture to: ${cacheFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }*/

        // Limpa o bitmap temporário criado se não for o bitmap original
        if (finalBitmap != bitmap) {
            finalBitmap.recycle()
        }

        // Localiza e atribui a textura no material do modelo GLB
        val asset = viewer.asset ?: return

        // Configura o Sampler de Textura
        val textureSampler = TextureSampler().apply {
            minFilter = TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR
            magFilter = TextureSampler.MagFilter.LINEAR
        }

        // Aplica a textura aos materiais correspondentes do livro
        for (entity in asset.entities) {
            val renderableManager = engine.renderableManager
            val instance = renderableManager.getInstance(entity)
            if (instance != 0) {
                val materialInstance = renderableManager.getMaterialInstanceAt(instance, 0)
                // Substitui o mapa albedo/baseColor do livro pela nossa textura
                materialInstance.setParameter("baseColorMap", texture, textureSampler)
            }
        }

        // Aguarda 2 frames para garantir que o Filament renderizou a nova textura no SurfaceView antes de notificar
        choreographer.postFrameCallback {
            if (!isDestroyed) {
                choreographer.postFrameCallback {
                    if (!isDestroyed) {
                        onReady?.invoke()
                    }
                }
            }
        }
    }

    private fun setupCamera() {
        val viewer = modelViewer ?: return
        // Posiciona a câmera do Filament levemente angulada
        val eyeX = 0.0
        val eyeY = 0.0
        val eyeZ = 3.5 // Retorna a câmera para uma distância segura para evitar cortes do clipping plane com o livro rotacionado
        val targetX = 0.0
        val targetY = 0.0
        val targetZ = 0.0
        val upX = 0.0
        val upY = 1.0
        val upZ = 0.0
        viewer.camera.lookAt(
            eyeX, eyeY, eyeZ,
            targetX, targetY, targetZ,
            upX, upY, upZ
        )
    }

    /**
     * Trata os gestos de drag do touch no fragment para rotacionar o livro no espaço 3D.
     * Consome o evento e solicita ao pai (NestedScrollView) para desabilitar a interceptação de scroll.
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        
        val viewer = modelViewer ?: return false
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                surfaceView.parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                surfaceView.parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        
        viewer.onTouchEvent(event)
        return true
    }

    private fun cleanup() {
        if (isDestroyed) return
        isDestroyed = true
        try {
            val viewer = modelViewer ?: return
            
            // Evitamos chamar viewer.destroy() porque ele chama destroyModel() que 
            // aciona asyncCancelLoad() e causa um Fatal Signal 11 (SIGSEGV) nativo.
            // Em vez disso, destruímos todos os recursos manualmente em ordem segura:
            
            val fResourceLoader = viewer.javaClass.getDeclaredField("resourceLoader")
            fResourceLoader.isAccessible = true
            val resourceLoader = fResourceLoader.get(viewer) as com.google.android.filament.gltfio.ResourceLoader

            val fAssetLoader = viewer.javaClass.getDeclaredField("assetLoader")
            fAssetLoader.isAccessible = true
            val assetLoader = fAssetLoader.get(viewer) as com.google.android.filament.gltfio.AssetLoader

            val fMaterialProvider = viewer.javaClass.getDeclaredField("materialProvider")
            fMaterialProvider.isAccessible = true
            val materialProvider = fMaterialProvider.get(viewer) as com.google.android.filament.gltfio.MaterialProvider

            resourceLoader.evictResourceData()

            val asset = viewer.asset
            if (asset != null) {
                viewer.scene.removeEntities(asset.entities)
                assetLoader.destroyAsset(asset)
                val fAsset = viewer.javaClass.getDeclaredField("asset")
                fAsset.isAccessible = true
                fAsset.set(viewer, null)
            }

            assetLoader.destroy()
            materialProvider.destroyMaterials()
            materialProvider.destroy()
            
            // INTENCIONALMENTE OMITIDO: resourceLoader.destroy()
            // Se chamarmos resourceLoader.destroy(), ele aciona internamente asyncCancelLoad() e o C++ quebra
            // com SEGV_MAPERR. Omitir isso causa um micro-vazamento de um ponteiro vazio, mas previne a falha crítica.

            // Chama detach() do UiHelper para destruir o SwapChain ANTES do Engine (previne IllegalStateException)
            val fUiHelper = viewer.javaClass.getDeclaredField("uiHelper")
            fUiHelper.isAccessible = true
            val uiHelper = fUiHelper.get(viewer) as com.google.android.filament.android.UiHelper
            uiHelper.detach()

            viewer.engine.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            modelViewer = null
        }
    }

    private inner class FrameCallback : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            choreographer.postFrameCallback(this)
            
            val viewer = modelViewer
            if (viewer != null) {
                val camera = viewer.camera
                val lm = viewer.engine.lightManager
                
                // Obtém a direção para onde a câmera está olhando (vetor forward)
                camera.getForwardVector(forwardVector)
                camera.getUpVector(upVector)
                
                // Calcula uma direção ligeiramente deslocada para baixo e direita da câmera para evitar luz muito reta
                // right = cross(forward, up)
                val rightX = forwardVector[1] * upVector[2] - forwardVector[2] * upVector[1]
                val rightY = forwardVector[2] * upVector[0] - forwardVector[0] * upVector[2]
                val rightZ = forwardVector[0] * upVector[1] - forwardVector[1] * upVector[0]
                
                // Luz frontal: olhando junto com a câmera, ligeiramente da direita e cima
                val fX = forwardVector[0] + 0.3f * rightX - 0.3f * upVector[0]
                val fY = forwardVector[1] + 0.3f * rightY - 0.3f * upVector[1]
                val fZ = forwardVector[2] + 0.3f * rightZ - 0.3f * upVector[2]
                
                val instFront = lm.getInstance(viewer.light)
                if (instFront != 0) {
                    lm.setDirection(instFront, fX, fY, fZ)
                }
                
                // Luz traseira: vindo da direção oposta à câmera (iluminando as costas)
                val bX = -forwardVector[0] - 0.3f * rightX + 0.3f * upVector[0]
                val bY = -forwardVector[1] - 0.3f * rightY + 0.3f * upVector[1]
                val bZ = -forwardVector[2] - 0.3f * rightZ + 0.3f * upVector[2]
                
                val instBack = lm.getInstance(backLightEntity)
                if (instBack != 0) {
                    lm.setDirection(instBack, bX, bY, bZ)
                }
                
                viewer.render(frameTimeNanos)
            }
        }
    }
}
