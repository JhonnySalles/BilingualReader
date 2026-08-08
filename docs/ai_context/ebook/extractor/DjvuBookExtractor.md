# DjvuBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [DjvuBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/DjvuBookExtractor.kt) fornece suporte para arquivos no formato de documento digitalizado **DjVu**. Ele utiliza a infraestrutura nativa do EBookDroid (`DjvuContext`) para inspecionar a estrutura do documento e obter informações de cabeçalho.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `djvu`.
* **Extração de Metadados:** O método `extractMetadata(path)` inicializa temporariamente uma instância de [DjvuContext](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/djvu/codec/DjvuContext.java) e abre o documento. Se for bem-sucedido, recupera `bookTitle` e `bookAuthor` diretamente do codec nativo do documento. Se o título for vazio, adota o nome do arquivo sem extensão como fallback. Libera os recursos do codec executando `recycle()`.
* **Conteúdo:** O método `extractContent` retorna uma instância de `BookContent.EpubFile(path)` contendo o caminho do arquivo original. Isso serve como um sinalizador para que o leitor carregue o arquivo DjVu diretamente em seu visualizador nativo sem conversão prévia.
* **Capa, Notas e Overview:** Não possui extração direta de imagens de capa, notas ou overview, retornando resultados nulos ou vazios.

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) populado via codec DjVu.
* `extractContent`: Retorna `BookContent.EpubFile(path)` apontando para o próprio arquivo DjVu.
