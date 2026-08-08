# OdtBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [OdtBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/OdtBookExtractor.kt) fornece suporte para arquivos no formato **ODT** (OpenDocument Text, padrão do LibreOffice/OpenOffice). Semelhante ao DOCX, trata-se de um contêiner ZIP contendo definições XML, permitindo extração estruturada do texto.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `odt`.
* **Metadados:** Abre o arquivo ZIP e localiza o arquivo `meta.xml` interno. Utiliza `XmlParser` para ler as tags `<dc:title>` ou `<title>` e `<dc:creator>` ou `<creator>`.
* **Extração de Conteúdo:** Localiza o arquivo `content.xml` dentro do contêiner ZIP e realiza o parse utilizando `XmlPullParser`:
  * **Parágrafos (`text:p`):** Lê e agrupa o texto dentro de tags `<p>`.
  * **Cabeçalhos (`text:h`):** Lê e agrupa o texto formatando-o como títulos `<h2>`.
* **Geração de HTML:** Compila todos os blocos de texto ordenadamente em um arquivo `odt-converted.html` no diretório de cache.

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) populado com título e autor.
* `extractContent`: Decomprime e processa os dados, gerando o arquivo HTML intermediário no cache e retornando um [BookContent.HtmlFile](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookContent.kt).
