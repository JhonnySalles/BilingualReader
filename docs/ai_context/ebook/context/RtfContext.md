# RtfContext

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [RtfContext](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/RtfContext.kt) fornece a ponte para carregamento e renderização de arquivos de texto formatados em RTF (Rich Text Format).

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Herança:** Estende `PdfContext`.
* **Gerenciamento de Caches:**
  * O método `getCacheFileName` calcula o hash a partir do caminho do arquivo e preferências de exibição (`isAutoHypens` e `hypenLang`), gerando o arquivo cache `<hash>.html`.
* **Processamento de Abertura:**
  * No método `openDocumentInner(fileName, password)`:
    1. Lança `IllegalStateException` caso seja invocado na thread principal de interface gráfica.
    2. Se o arquivo cache `.html` não estiver presente, executa o [RtfBookExtractor](file:///e:/Proj/BilingualReader/docs/ai_context/ebook/extractor/RtfBookExtractor.md) em um bloco `runBlocking` direcionado à thread `EbookDispatcher.dispatcher`.
    3. Copia o arquivo HTML temporário gerado para a estrutura persistente de cache (`<hash>.html`) e remove o temporário original.
    4. Abre o arquivo HTML de cache final utilizando o `MuPdfDocument` nativo.

## 🗂️ Padrão e Retorno
Retorna uma instância de `MuPdfDocument` associada ao arquivo HTML persistido no cache.
