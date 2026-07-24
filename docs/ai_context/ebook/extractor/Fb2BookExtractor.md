# Fb2BookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [Fb2BookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/Fb2BookExtractor.kt) fornece suporte para arquivos no formato **FictionBook 2.0** (`.fb2`). Sendo o FB2 um formato baseado puramente em XML contendo metadados, texto estruturado e imagens codificadas em Base64 em um único arquivo, o extrator realiza o parse XML completo da obra e a converte dinamicamente em um pacote EPUB padrão.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `fb2`.
* **Codificação:** Utiliza `findHeaderEncoding(path)` para ler a primeira linha do arquivo XML (ex: `<?xml version="1.0" encoding="windows-1251"?>`) e determinar o encoding correto.
* **Metadados:** Lê tags da seção `<title-info>` do XML (como `<book-title>`, `<lang>`, `<first-name>`, `<last-name>`, `<genre>`, `<sequence>`, `<isbn>`, `<publisher>`, `<date>`).
* **Imagem de Capa:** Varre a tag `<binary id="...">` do XML. O conteúdo textual interno (que é uma imagem codificada em Base64) é decodificado em um `ByteArray`. A capa prioritária é detectada por IDs que contêm a palavra "cover" ou, na falta desta, a primeira imagem binária encontrada.
* **Sinopse (Overview):** Extrai o texto contido na tag `<annotation>` de forma limpa.
* **Notas de Rodapé:** Varre referências cruzadas `<a type="note" l:href="...">` e links internos que apontam para seções marcadas como notas de rodapé (`section id="..."` contendo texto explicativo), montando um mapa de ID para nota.
* **Conversão Dinâmica para EPUB (`convert`):**
  * Para renderização eficaz pelo visualizador base (MuPDF/EbookDroid), o arquivo `.fb2` é transformado em um arquivo `.epub` temporário de cache.
  * Cria uma estrutura de arquivo ZIP contendo:
    * `mimetype` (`application/epub+zip`).
    * `META-INF/container.xml`.
    * Imagem de capa decodificada e injetada (`OEBPS/bilingual-cover-image.jpg` e `OEBPS/bilingual-cover-page.xhtml`).
    * Imagens internas extraídas dos blocos `<binary>`.
    * O arquivo `content.opf` e o sumário `fb2.ncx` gerados dinamicamente usando templates em [Fb2Templates](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/util/Fb2Templates.kt).
    * O próprio arquivo XML convertido em `fb2.fb2`.

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) populado com os dados do FictionBook XML.
* `extractContent`: Converte o arquivo FB2 em EPUB temporário, retornando um [BookContent.EpubFile](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookContent.kt) apontando para o arquivo de cache gerado.
