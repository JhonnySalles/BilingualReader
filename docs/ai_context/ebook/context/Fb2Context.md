# Fb2Context

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [Fb2Context](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/Fb2Context.kt) faz a ponte entre o visualizador MuPDF e a engine de processamento de e-books no formato FictionBook `.fb2`. Como arquivos FB2 puros são XMLs, ele orquestra a conversão prévia deles para EPUB (que é interpretado como PDF/HTML pelo visualizador) e resolve as notas de rodapé de forma assíncrona.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Herança:** Estende `PdfContext`.
* **Gerenciamento de Caches:**
  * O método `getCacheFileName` calcula um hash a partir do caminho do arquivo original e de preferências de exibição (`isAutoHypens`, `hypenLang` e `AppState.isDouble`).
  * Define dois caminhos de arquivo cache possíveis: `hash.epub` e `hash.epub.fb2`.
* **Processamento de Abertura:**
  * No método `openDocumentInner(fileName, password)`:
    1. Se houver algum dos arquivos cache (`.epub` ou `.epub.fb2`) já gravados, utiliza-o.
    2. Caso contrário, invoca `Fb2BookExtractor.convert` para empacotar o XML do FB2 em um EPUB estruturado.
    3. Abre o arquivo final com `MuPdfDocument`.
    4. **Tratamento de Falhas (Double Attempt):** Se a abertura falhar (gerando exceção), deleta o cache corrompido e tenta uma segunda conversão usando o método alternativo `Fb2BookExtractor.convertFB2`, abrindo novamente com o `MuPdfDocument`.
    5. **Notas de Rodapé:** Se existir um arquivo JSON correspondente às notas no cache, carrega-o e associa via `setFootNotes`. Caso contrário, dispara uma corrotina assíncrona (`CoroutineScope(Dispatchers.IO)`) para extrair as notas do FB2 original (`Fb2BookExtractor.extractFooterNotes`), gravar o arquivo JSON de cache e associá-las ao documento, seguida por `removeTempFiles()`.

## 🗂️ Padrão e Retorno
Retorna um `MuPdfDocument` referenciando o EPUB gerado a partir do FB2, com as notas de rodapé associadas ao documento.
