# MobiBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [MobiBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/MobiBookExtractor.kt) fornece suporte avançado a e-books nos formatos da Amazon/Mobipocket: **MOBI**, **AZW**, **AZW3**, **AZW4**, além dos formatos genéricos de banco de dados Palm OS **PDB** e **PRC**. Devido às particularidades desses formatos binários, ele integra rotinas de baixo nível com a biblioteca nativa `LibMobi` e parsers internos de fluxo de dados.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `mobi`, `azw`, `azw3`, `azw4`, `pdb`, `prc`.
* **Leitura de Metadados e Capas:**
  * Utiliza mapeamento de arquivos em memória (`FileChannel.MapMode.READ_ONLY`) para obter um `ByteBuffer` de leitura rápida.
  * O [MobiParser](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/foobnix/mobi/parser/MobiParser.java) lê a estrutura interna do cabeçalho binário (PDB/PalmDOC) e recupera dados como título, autor, assunto (gênero), idioma, ISBN, editora e imagem de capa (`getCoverOrThumb()`).
* **Extração de Conteúdo (Duplo Fluxo de Conversão):**
  1. **Conversão Nativa via `LibMobi`:** Tenta invocar a biblioteca nativa `LibMobi.convertToEpub(tempFile, destPath)` para descompilar e recomilar a estrutura MOBI em um arquivo `.epub` padrão. Se for bem-sucedido, injeta a capa extraída no novo EPUB via `EpubCoverInjector.injectCover` e retorna um [BookContent.EpubFile](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookContent.kt).
  2. **Fallback PalmDOC:** Se a biblioteca nativa falhar ou o arquivo for no formato legado `.pdb` (não suportado pelo `LibMobi`), o extrator recorre ao [PalmDocDecompressor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/util/PalmDocDecompressor.java). O fluxo descompactado é decodificado com o charset `cp1252`, e cada linha é convertida em um documento HTML limpo com parágrafos `<p>` e quebras `<br/>`, sendo retornado como um [BookContent.HtmlFile](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookContent.kt).

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) obtido do `MobiParser`.
* `extractContent`: Converte para EPUB (retornando `BookContent.EpubFile`) ou gera HTML via PalmDOC (retornando `BookContent.HtmlFile`) no diretório temporário.
