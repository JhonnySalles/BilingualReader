# CalibreBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [CalibreBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/CalibreBookExtractor.kt) atua como um extrator e leitor de metadados gerados pelo organizador de e-books **Calibre**. Ele não implementa a interface `BookExtractor` diretamente, mas serve como um utilitário de suporte usado pela fábrica `BookExtractorFactory` para detectar e extrair metadados mais ricos quando um arquivo `metadata.opf` gerado pelo Calibre está presente no mesmo diretório do arquivo de e-book original.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Detecção:** O método `isCalibre(path)` verifica se existe um arquivo chamado `metadata.opf` no diretório pai (`parentFile`) do arquivo de livro sendo processado.
* **Extração de Sinopse (Overview):** O método `getBookOverview(path)` abre e lê o arquivo `metadata.opf` usando o `XmlParser` do Foobnix para encontrar a tag `<dc:description>`. O texto resultante é sanitizado usando `Jsoup.clean(text, Safelist.simpleText())`.
* **Extração de Metadados Completos:** O método `getBookMetaInformation(path)` realiza o parse do `metadata.opf` e recupera:
  * `<dc:title>`: Título do livro.
  * `<dc:creator>`: Autor do livro (com suporte a formatação "Sobrenome, Nome" dependendo do `EbookSettings.isFirstSurname`).
  * `<dc:description>`: Descrição/anotação da obra.
  * `<dc:identifier>`: ISBN (recuperado de identificadores que contêm a string "isbn" e limpo com expressão regular).
  * `<dc:publisher>`: Editora.
  * `<dc:date>`: Data de lançamento.
  * `<meta name="calibre:series">` e `<meta name="calibre:series_index">`: Nome e índice da série/volume do livro.
  * `<reference type="cover" href="...">`: Imagem de capa referenciada, lendo os bytes diretamente do arquivo de imagem apontado no href na mesma pasta.

## 🗂️ Padrão e Retorno
Retorna uma instância populada de [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) contendo todas as informações colhidas no arquivo OPF, com a imagem de capa em formato de `ByteArray` em `coverImage` caso seja encontrada.
