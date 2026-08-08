# MarkdownBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [MarkdownBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/MarkdownBookExtractor.kt) gerencia arquivos de texto formatados em **Markdown** (`.md` ou `.markdown`). Ele implementa um parser de Markdown estruturado em Kotlin de nível básico para converter a formatação simplificada em HTML representável na visualização do leitor.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `md`, `markdown`.
* **Metadados:**
  1. Varre o arquivo em busca de um bloco Frontmatter (YAML) delimitado por `---` no início do arquivo. Se encontrar, separa chaves e valores para extrair `title` e `author`.
  2. Fora do Frontmatter (ou caso não exista), tenta capturar o primeiro cabeçalho de nível 1 (`# Título`) como título da obra.
  3. Se falhar em ambos, adota o nome do arquivo sem extensão como fallback.
* **Processamento de Conteúdo:** O extrator lê o arquivo linha a linha:
  * **Listas:** Agrupa itens que começam com `- `, `* ` ou `+ ` em blocos HTML `<ul>` e `<li>`.
  * **Cabeçalhos:** Converte `#`, `##` e `###` em `<h1>`, `<h2>` e `<h3>` respectivamente.
  * **Citações:** Converte linhas iniciadas em `> ` em blocos `<blockquote>`.
  * **Parágrafos:** Linhas padrão são envoltas em tags `<p>`.
  * **Formatação Inline:** Utiliza expressões regulares para formatar:
    * **Negrito (`**texto**`):** Substituído por `<b>texto</b>`.
    * **Itálico (`*texto*` ou `_texto_`):** Substituído por `<i>texto</i>`.
* **Geração de HTML:** Compila todo o resultado em um arquivo intermediário `markdown-converted.html` no diretório de cache.

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) populado com os dados coletados.
* `extractContent`: Salva o HTML compilado no cache e retorna uma instância de [BookContent.HtmlFile](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookContent.kt).
