# DocxBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [DocxBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/DocxBookExtractor.kt) gerencia arquivos modernos do Microsoft Word no formato **DOCX** (Office Open XML). Sendo um arquivo compactado (ZIP) contendo arquivos XML estruturados, o extrator realiza o descompactamento e lê diretamente as definições internas do documento.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `docx`.
* **Metadados:** Abre o ZIP do DOCX e lê o arquivo `docProps/core.xml` via `XmlParser`. Extrai o título (`dc:title` ou `title`) e criador (`dc:creator` ou `creator`).
* **Mapeamento de Imagens (Relations):** Lê o arquivo `word/_rels/document.xml.rels` para mapear os IDs de relacionamento (`rId`) associados a arquivos do tipo imagem. Em seguida, copia os arquivos correspondentes contidos na pasta interna `word/media` para a pasta de cache do livro.
* **Processamento de Conteúdo:** O extrator realiza o parse do documento XML principal `word/document.xml` utilizando `XmlPullParser`:
  * **Parágrafos (`w:p`):** Lê e concatena os textos.
  * **Estilo de Título (`w:pStyle`):** Se o estilo começar com "Heading" (ignora case), trata o parágrafo atual como cabeçalho `<h2>`.
  * **Imagens incorporadas (`a:blip` / `blip`):** Lê o atributo de embed (geralmente `r:embed` ou finalizado em `embed`), recupera o caminho da imagem correspondente no mapa de relações, e insere a tag `<img src="path/to/media" />`.
* **Geração de HTML:** Toda a estrutura de parágrafos, cabeçalhos e tags de imagem é compilada no arquivo intermediário `docx-converted.html`.

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) extraído do XML core.
* `extractContent`: Salva o HTML resultante no diretório de destino e retorna um [BookContent.HtmlFile](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookContent.kt) com o caminho absoluto do arquivo.
