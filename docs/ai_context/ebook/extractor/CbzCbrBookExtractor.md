# CbzCbrBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [CbzCbrBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/CbzCbrBookExtractor.kt) gerencia arquivos de quadrinhos compactados (Comic Book Archive) nos formatos **CBZ**, **CBR** e arquivos **ZIP** gerais. Ele implementa a interface `BookExtractor` focando no tratamento desses arquivos como pacotes de imagens sequenciais.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `cbz`, `cbr`, `zip`.
* **Detecção de ZIP:** Possui o método utilitário `isZip(path)` que lê os primeiros dois bytes do arquivo para verificar a assinatura `"PK"` comum dos cabeçalhos ZIP.
* **Extração de Capa:** O método `extractCover(path)` abre o arquivo ZIP (usando `ZipFile` com encoding UTF-8) e itera sobre todas as entradas de arquivos. Ele filtra as entradas que são imagens (`ExtUtils.isImagePath`), ordena alfabeticamente os nomes das imagens, lê os bytes da primeira imagem encontrada (que representa a capa) e os retorna como `ByteArray`.
* **Metadados:** O método `extractMetadata(path)` preenche apenas o título usando o nome do arquivo ZIP como fallback, deixando o autor vazio.
* **Conteúdo e Notas:** Não suporta extração direta de conteúdo para arquivos HTML normais, falhando intencionalmente com `UnsupportedOperationException("CBZ/CBR content extraction not supported")`. Tampouco suporta notas de rodapé ou visões gerais.

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) com `title` igual ao nome do arquivo.
* `extractCover`: Retorna um `ByteArray` da primeira imagem encontrada dentro do pacote ZIP/CBZ.
