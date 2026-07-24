# DocContext

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [DocContext](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/DocContext.kt) atua como a ponte entre o visualizador de leitura (baseado no MuPDF) e a lógica de processamento de documentos Microsoft Word `.doc`. Ele gerencia a inicialização e o ciclo de vida do carregamento de arquivos DOC.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Herança:** Herda de `PdfContext`, tirando proveito da capacidade do visualizador MuPDF de renderizar arquivos HTML (após a conversão).
* **Processamento de Abertura:**
  * No método `openDocumentInner(fileName, password)`, verifica se a operação não está rodando na thread principal do Android (lançando `IllegalStateException` caso positivo, para evitar travamento da UI).
  * Invoca a fábrica de extração `BookExtractorFactory.getExtractor(fileName)` ou adota o fallback [DocBookExtractor](file:///e:/Proj/BilingualReader/docs/ai_context/ebook/extractor/DocBookExtractor.md).
  * Executa a extração assíncrona do conteúdo via `extractContent`, gerando o arquivo HTML estruturado equivalente com imagens convertidas na pasta de cache.
  * O arquivo HTML gerado é aberto pelo motor MuPDF na forma de um [MuPdfDocument](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/mupdf/codec/MuPdfDocument.java), permitindo a paginação fluida e renderização do livro na tela.

## 🗂️ Padrão e Retorno
Retorna uma instância de `MuPdfDocument` referenciando o arquivo HTML intermediário gerado pelo `DocBookExtractor`.
