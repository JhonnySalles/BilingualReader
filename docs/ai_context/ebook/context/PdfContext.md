# PdfContext

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [PdfContext](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/mupdf/codec/PdfContext.java) fornece a base de renderização nativa baseada na biblioteca **MuPDF** para arquivos PDF e XPS, além de servir como classe mãe comum para todos os droids de e-books que realizam conversão para HTML ou EPUB intermediários.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Herança:**
  * Estende `MuPdfContext` (que por sua vez estende `AbstractCodecContext` da biblioteca principal do EBookDroid).
* **Configurações Globais de Renderização (definidas em `MuPdfContext`):**
  * **Configuração de Bitmap:** O método `getBitmapConfig()` força a utilização do padrão ARGB de 32 bits (`Bitmap.Config.ARGB_8888`) para garantir cores e nitidez ideais durante o rendering de texto e imagens.
  * **Acesso Paralelo:** O método `isParallelPageAccessAvailable()` retorna falso, sinalizando ao leitor que os acessos nativos à renderização das páginas devem ocorrer de forma sequencial (thread-safe), protegendo contra race conditions nas chamadas JNI do MuPDF.
* **Processamento de Abertura:**
  * O método `openDocumentInner(fileName, password)` constrói e retorna uma instância do motor do visualizador [MuPdfDocument](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/mupdf/codec/MuPdfDocument.java) parametrizado com o formato de arquivo PDF (`MuPdfDocument.FORMAT_PDF`).

## 🗂️ Padrão e Retorno
Retorna uma instância nativa do `MuPdfDocument` configurada para renderizar o fluxo PDF do arquivo solicitado.
