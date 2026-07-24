# HtmlContext

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [HtmlContext](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/HtmlContext.kt) faz a ponte de exibição para arquivos nos formatos HTML/XHTML brutas. Ele extrai, limpa e hifeniza a página antes de passá-la para o visualizador MuPDF.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Herança:** Estende `PdfContext`.
* **Processamento de Abertura:**
  * No método `openDocumentInner(fileName, password)`:
    1. Lança `IllegalStateException` caso seja invocado na thread principal de interface gráfica.
    2. Utiliza `BookExtractorFactory.getExtractor` (ou adota fallback [HtmlBookExtractor](file:///e:/Proj/BilingualReader/docs/ai_context/ebook/extractor/HtmlBookExtractor.md)) para realizar a extração/sanitização do documento.
    3. Executa a extração em um escopo de corrotina bloqueante (`runBlocking`) direcionado para a thread de I/O do leitor (`EbookDispatcher.dispatcher`), gerando o arquivo `temp.html` higienizado na pasta de cache.
    4. Abre o arquivo HTML resultante com o `MuPdfDocument` nativo.
    5. Associa as notas de rodapé extraídas ao documento.

## 🗂️ Padrão e Retorno
Retorna uma instância de `MuPdfDocument` associada ao arquivo HTML processado.
