# DjvuContext

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [DjvuContext](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/djvu/codec/DjvuContext.java) gerencia a inicialização, carregamento de documentos e ciclo de vida de recursos de baixo nível/nativos associados à renderização de arquivos DjVu.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Herança:** Estende `AbstractCodecContext` diretamente (sem passar por `PdfContext`), pois possui engine e parser de decodificação nativa próprios para o formato DjVu.
* **Pontes Nativas (JNI):**
  * Invoca funções em código nativo C/C++ utilizando chamadas locais:
    * `private static native long create()`: Cria e retorna a referência/ponteiro do contexto nativo.
    * `private static native void free(long contextHandle)`: Libera os buffers de memória alocados nativamente para o documento.
  * Todas as operações nativas críticas (criação e destruição) são sincronizadas utilizando a trava estática de sistema `TempHolder.lock.lock()`.
* **Processamento de Abertura:**
  * O método `openDocumentInner(fileName, password)` instancia e retorna uma referência a `DjvuDocument(this, fileName)` associada ao contexto nativo construído.
* **Liberação de Recursos:**
  * Implementa `freeContext()` executando a limpeza nativa de memória do handle via `free(getContextHandle())` sob a proteção da trava `TempHolder.lock`.

## 🗂️ Padrão e Retorno
Retorna uma instância de `DjvuDocument` nativa inicializada para o arquivo DjVu.
