# PdfBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [PdfBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/PdfBookExtractor.kt) fornece suporte de metadados e controle de leitura para arquivos nos formatos **PDF** e **XPS**. Ele utiliza o codec do visualizador nativo (`PdfContext` encapsulando MuPDF) para obter informações sobre o documento.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `pdf`, `xps`.
* **Metadados:** Abre temporariamente o documento utilizando [PdfContext](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/mupdf/codec/PdfContext.java) com senha em branco. Extrai as propriedades nativas `bookTitle` e `bookAuthor` providas pelo arquivo PDF. Garante a liberação de memória chamando `recycle()` no documento e no contexto do codec. Caso o título venha vazio, utiliza o nome do arquivo sem extensão como fallback.
* **Imagem de Capa:** Retorna `null`. Arquivos PDF não contêm um fluxo isolado de imagem de capa nos padrões de e-book. A capa do PDF é renderizada de forma dinâmica pelo visualizador usando a página indexada como 0 (primeira página).
* **Conteúdo:** O método `extractContent` retorna uma instância de [BookContent.EpubFile](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookContent.kt) apontando para o próprio arquivo PDF original. O visualizador do aplicativo identificará a extensão e abrirá o arquivo diretamente pelo motor MuPDF nativo sem necessidade de conversão para HTML.

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) obtido do codec.
* `extractContent`: Retorna `BookContent.EpubFile(path)` apontando para o próprio arquivo PDF original.
