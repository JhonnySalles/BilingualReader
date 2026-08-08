# EpubBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [EpubBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/EpubBookExtractor.kt) gerencia arquivos nos formatos **EPUB** e **KEPUB** (Kobo EPUB). Sendo o principal formato de livro dinâmico suportado, ele implementa um fluxo avançado de análise e pré-processamento estrutural para garantir que os arquivos sigam padrões ótimos de legibilidade e vinculação de elementos no visualizador.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `epub`, `kepub`.
* **Metadados:** Lê o arquivo `.opf` contido dentro do arquivo ZIP para preencher o [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) (título, autor, série, gênero, editora, data de lançamento, etc.).
* **Extração de Capa:**
  1. Busca no manifesto do `.opf` por tags de metadados como `<meta name="cover" content="..." />` ou itens com propriedade `cover-image`.
  2. Caso não existam metadados explícitos, faz uma varredura interna no ZIP buscando imagens com nomes comuns como `cover.jpg`, `cover.png`, `cover.jpeg`, `folder.jpg`, etc.
* **Pré-processamento (`preprocessEpub`):**
  * Corrige referências do arquivo OPF, injetando links de sumário NCX se estiverem ausentes da tag `spine`.
  * **Capa Bilíngue:** Se o livro não possuir uma página de capa XHTML, injeta uma página customizada (`bilingual-cover-page.xhtml`) estilizada com CSS centralizado e responsivo.
  * **SVG Cover Normalization:** Se a página de capa usar elementos `<svg>` com imagem interna embutida, converte a página para usar a tag padrão `<img>` do HTML para garantir renderização perfeita.
  * **Hifenização:** Caso a hifenização automática (`BookCSS.get().isAutoHypens`) esteja ativa, itera sobre todos os arquivos HTML/XHTML internos do livro e aplica as regras de hifenização da língua correspondente via `Fb2BookExtractor.generateHyphenFile`.
* **Extração de Notas de Rodapé:** O método `extractFooterNotes` lê os arquivos HTML internos buscando tags `<a>` de tipo `note` ou que possuam referências cruzadas que apontem para seções de notas de rodapé, extraindo-as para mapeamento dinâmico.
* **Anexos de Mídia (`getAttachments`):** Extrai arquivos de áudio/vídeo embutidos no EPUB (como MP3, MP4) para a pasta de cache temporária.

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt).
* `extractContent`: Realiza a cópia e pré-processamento gerando o arquivo EPUB otimizado no diretório temporário, retornando um [BookContent.EpubFile](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookContent.kt).
