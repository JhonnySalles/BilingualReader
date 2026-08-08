# DocBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [DocBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/DocBookExtractor.kt) gerencia arquivos legados do Microsoft Word no formato binário **DOC** (estruturas OLE2). Devido à complexidade e ausência de parser de alto nível no Android para esse formato legado, ele implementa um leitor de fluxo binário personalizado para extrair texto legível e imagens incorporadas diretamente dos bytes brutos.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `doc`.
* **Extração de Imagens:** O extrator varre o array de bytes binários do arquivo `.doc` buscando assinaturas mágicas de arquivos de imagem:
  * **JPEG:** Cabeçalho `FF D8 FF` e rodapé `FF D9`.
  * **PNG:** Cabeçalho `89 50 4E 47` e rodapé `49 45 4E 44 AE 42 60 82`.
  * Se o tamanho do fragmento binário for maior que 2048 bytes (para evitar ruídos menores), ele grava o arquivo de imagem em cache (`image_{index}.jpg/png`) e armazena seu caminho para posterior inserção no HTML.
* **Extração de Texto:** O arquivo é varrido caractere por caractere em busca de dois padrões de codificação:
  * **UTF-16LE:** Caracteres comuns em documentos do Word. Se encontrar sequências válidas maiores que 8 bytes, decodifica o trecho.
  * **US-ASCII/Windows-1252:** Sequências imprimíveis maiores que 6 bytes (ignorando assinaturas do sistema como "Microsoft" ou "Word.Document").
* **Ancoragem de Imagens:** O caractere especial `\u0001` (0x01) dentro do fluxo de texto representa a posição de uma imagem incorporada. O extrator quebra o parágrafo neste ponto e injeta a tag `<img src="image_{index}.png" />` correspondente.
* **Geração de HTML:** Compila o texto estruturado em parágrafos `<p>` e imagens dentro do arquivo `doc-converted.html` no diretório de cache especificado.

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) usando o nome do arquivo como título padrão.
* `extractContent`: Salva o HTML com o texto extraído e imagens vinculadas em `doc-converted.html`, retornando um `BookContent.HtmlFile`.
