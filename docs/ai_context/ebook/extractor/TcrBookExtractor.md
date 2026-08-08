# TcrBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [TcrBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/TcrBookExtractor.kt) fornece suporte para arquivos no formato **TCR** (formato de e-book legado compactado, popular em dispositivos Psion). Devido ao algoritmo de compressão proprietário, o extrator implementa um descompressor baseado em dicionário de 8 bits diretamente em Kotlin para ler o conteúdo da obra.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `tcr`.
* **Algoritmo de Descompressão TCR:**
  1. Ignora os primeiros 9 bytes de cabeçalho do arquivo.
  2. Inicializa um dicionário contendo 256 entradas de arrays de bytes (`ByteArray`).
  3. Lê as próximas 256 entradas do arquivo: cada entrada inicia com 1 byte contendo a extensão da palavra do dicionário, seguido pelos bytes correspondentes.
  4. O restante do arquivo consiste em chaves de indexação de 1 byte. Para cada byte lido, o extrator pesquisa e escreve a sequência de bytes correspondente indexada no dicionário.
  5. Decodifica o fluxo de bytes resultante como texto UTF-8.
* **Geração de HTML:**
  * Cria o arquivo `tcr-converted-<hash>.html`.
  * Divide o texto por quebras de linha (`\n`). Linhas vazias são transformadas em tags `<br/>`. Linhas com texto são protegidas contra HTML malicioso (`htmlEncode`) e envolvidas em tags `<p>`.

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) básico usando o nome do arquivo.
* `extractContent`: Descompacta o arquivo binário TCR e gera um arquivo HTML correspondente no cache, retornando um [BookContent.HtmlFile](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookContent.kt).
