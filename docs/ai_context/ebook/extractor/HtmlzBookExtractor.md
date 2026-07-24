# HtmlzBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [HtmlzBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/HtmlzBookExtractor.kt) fornece suporte para arquivos no formato **HTMLZ** (HTML Compactado em ZIP, tipicamente criado pelo software Calibre). Ele trata o arquivo como um pacote ZIP contendo uma página web principal (`index.html`), metadados OPF e recursos de imagens.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `htmlz`.
* **Metadados:** Abre o arquivo ZIP e realiza o parse do arquivo `metadata.opf` interno utilizando `XmlParser`. Extrai informações de título e autor (`dc:title` / `dc:creator`).
* **Imagem de Capa:**
  1. Busca uma entrada explícita com o nome `cover.jpg`, `cover.png` ou `cover.jpeg`.
  2. Se não existir, varre todas as entradas e retorna os bytes do primeiro arquivo que corresponda a uma extensão de imagem válida (`.jpg`, `.jpeg`, `.png`, `.webp`, `.gif`).
* **Extração de Conteúdo:**
  1. Cria uma pasta temporária estruturada como `htmlz_temp_<hash_do_caminho>`.
  2. Decomprime recursivamente todas as pastas e arquivos contidos no arquivo ZIP para este diretório.
  3. Localiza o arquivo principal `index.html`. Caso este arquivo esteja ausente, lança uma exceção `IOException`.

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) populado via metadados OPF.
* `extractCover`: Retorna os bytes (`ByteArray`) do arquivo de capa extraído do ZIP.
* `extractContent`: Retorna um [BookContent.HtmlFile](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookContent.kt) apontando para o caminho absoluto do arquivo `index.html` descompactado no cache.
