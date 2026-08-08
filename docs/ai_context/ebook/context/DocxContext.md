# DocxContext

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [DocxContext](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/DocxContext.kt) fornece a ponte para carregamento e renderização de documentos Microsoft Word modernos no formato `.docx`.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Herança:** Herda de `PdfContext`, aproveitando as APIs do MuPDF para processar arquivos estruturados em HTML.
* **Processamento de Abertura:**
  * No método `openDocumentInner(fileName, password)`, garante que a execução ocorra fora da thread principal de UI.
  * Seleciona o extrator via `BookExtractorFactory` (ou fallback [DocxBookExtractor](file:///e:/Proj/BilingualReader/docs/ai_context/ebook/extractor/DocxBookExtractor.md)).
  * Dispara a corrotina bloqueante de extração para converter as tags XML e extrair imagens de mídia do DOCX em um arquivo `docx-converted.html` temporário.
  * Instancia e inicializa o documento final usando o motor do `MuPdfDocument` referenciando o arquivo HTML de cache.

## 🗂️ Padrão e Retorno
Retorna uma instância de `MuPdfDocument` referenciando o arquivo HTML intermediário gerado pelo `DocxBookExtractor`.
