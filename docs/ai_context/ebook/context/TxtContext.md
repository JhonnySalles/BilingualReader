# TxtContext

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [TxtContext](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/TxtContext.kt) fornece a ponte para carregamento e renderização de arquivos de texto plano brutos (.txt).

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Herança:** Estende `PdfContext`.
* **Processamento de Abertura:**
  * No método `openDocumentInner(fileName, password)`:
    1. Lança `IllegalStateException` caso seja invocado na thread principal de interface gráfica.
    2. Utiliza `BookExtractorFactory.getExtractor` (ou adota fallback [TxtBookExtractor](file:///e:/Proj/BilingualReader/docs/ai_context/ebook/extractor/TxtBookExtractor.md)) para realizar o processamento e formatação do arquivo de texto.
    3. Executa a extração em corrotina síncrona/bloqueante (`runBlocking`) utilizando `EbookDispatcher.dispatcher` para gerar o arquivo `txt.html` de cache.
    4. Abre o arquivo HTML resultante com a classe `MuPdfDocument`.
    5. Associa as notas de rodapé extraídas ao documento.

## 🗂️ Padrão e Retorno
Retorna uma instância de `MuPdfDocument` associada ao arquivo HTML processado.
