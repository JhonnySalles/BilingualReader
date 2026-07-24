# EpubContext

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [EpubContext](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/EpubContext.kt) atua como o principal orquestrador de ciclo de vida e renderização de livros nos formatos EPUB e KEPUB. Ele herda de `PdfContext` (abrindo o arquivo tratado via motor do MuPDF), gerenciando corrotinas assíncronas para extrair metadados pesados, notas de rodapé e anexos de mídia sem travar a interface de leitura do usuário.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Herança:** Estende `PdfContext`.
* **Gerenciamento de Caches:**
  * O método `getCacheFileName(fileNameOriginal)` calcula um hash combinando o caminho do arquivo com as configurações de hifenização atual (`isAutoHypens` e `hypenLang`). Cria um arquivo cache `.epub` temporário correspondente.
* **Processamento de Abertura:**
  * No método `openDocumentInner(fileName, password)`:
    1. Se o arquivo pré-processado de cache não existir, invoca `EpubBookExtractor.preprocessEpub(fileName, cache.path)` para higienizar e estruturar o livro.
    2. Instancia o `MuPdfDocument` carregando o arquivo de cache gerado.
    3. Verifica se existe um arquivo `.json` de notas no cache. Caso sim, lê e atribui ao documento usando `muPdfDocument.setFootNotes(JsonHelper.fileToMap(jsonFile))`.
    4. Abre um escopo assíncrono (`CoroutineScope(Dispatchers.IO + SupervisorJob())`) para processar tarefas pesadas de fundo:
       * Associa anexos de mídia (áudios/vídeos) ao documento via `muPdfDocument.setMediaAttachment(EpubBookExtractor.getAttachments(fileName))`.
       * Se o arquivo JSON de notas não existia, dispara `EpubBookExtractor.extractFooterNotes(fileName)`. Se encontrar notas, atribui-as ao documento e grava o arquivo JSON no cache para consultas futuras.
       * Executa `removeTempFiles()` para manter o espaço limpo.
* **Liberação de Recursos:** Sobrescreve `freeContext()` para cancelar o escopo de corrotinas (`scope.cancel()`) e limpar objetos de I/O em andamento.

## 🗂️ Padrão e Retorno
Retorna uma instância populada de `MuPdfDocument` associada ao EPUB pré-processado e com notas de rodapé e recursos multimídia injetados dinamicamente.
