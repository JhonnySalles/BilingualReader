# RtfBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [RtfBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/RtfBookExtractor.kt) gerencia arquivos no formato **RTF** (Rich Text Format). Ele realiza o parse do formato estruturado em texto enriquecido utilizando a biblioteca externa `rtfparserkit-1.10.0.jar`.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `rtf`.
* **Extração de Capa:** Abre o arquivo RTF e realiza um parse raso utilizando `StandardRtfParser`. Escuta eventos de imagens (`pngblip` ou `jpegblip`) e converte a string hexadecimal associada em `ByteArray` via `HexUtils.parseHexString`. Adota a primeira imagem encontrada como capa.
* **Extração de Conteúdo:** O extrator converte o documento RTF em HTML gravando no arquivo `rtf_temp_<hash>.html`:
  * **Hifenização:** Inicializa a língua de hifenização de acordo com `EbookSettings` caso esteja habilitada.
  * **Parser de Texto:** Implementa uma extensão de `StringTextConverter` do rtfparserkit:
    * **Texto Extraído:** Codifica o texto em HTML (`TextUtils.htmlEncode`) e aplica hifenização se habilitado, antes de escrevê-lo.
    * **Imagens incorporadas:** Quando um comando `pngblip` ou `jpegblip` é disparado, lê a string hexadecimal de dados de imagem subsequente, decodifica-a e grava-a em um arquivo de imagem separado (`rtf_temp_<hash>.html_<counter>.rtf.png/jpg`) no cache, inserindo o marcador `<img src="..." />` no HTML.
    * **Quebras de parágrafo:** Comandos `cbpat`, `par` e `line` são mapeados para tags `<br/>` no HTML (com flag de controle `isBR` para evitar quebras consecutivas repetidas).

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) básico com o nome do arquivo.
* `extractContent`: Converte a estrutura RTF (texto + imagens) em um arquivo HTML e retorna uma instância de [BookContent.HtmlFile](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookContent.kt).
