# OdtContext

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [OdtContext](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/OdtContext.kt) fornece a ponte para carregamento e renderização de arquivos de texto formatados em ODT (OpenDocument Text, padrão LibreOffice).

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Herança:** Estende `PdfContext`.
* **Processamento de Abertura:**
  * No método `openDocumentInner(fileName, password)`:
    1. Lança `IllegalStateException` caso seja invocado na thread principal de interface gráfica.
    2. Invoca o [OdtBookExtractor](file:///e:/Proj/BilingualReader/docs/ai_context/ebook/extractor/OdtBookExtractor.md) para realizar a extração e conversão do arquivo ODT.
    3. Executa a conversão em corrotina síncrona/bloqueante (`runBlocking`) utilizando `EbookDispatcher.dispatcher` para gerar o arquivo `odt-converted.html` de cache.
    4. Abre o arquivo HTML resultante com a classe `MuPdfDocument`.
    5. Associa as notas de rodapé extraídas ao documento.

## 🗂️ Padrão e Retorno
Retorna uma instância de `MuPdfDocument` associada ao arquivo HTML gerado.
