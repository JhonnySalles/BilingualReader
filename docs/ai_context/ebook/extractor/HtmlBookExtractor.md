# HtmlBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [HtmlBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/HtmlBookExtractor.kt) gerencia arquivos de hipertexto nos formatos **HTML**, **HTM**, **XHTML**, **XHTM**, **MHT** e **MHTML**. Ele atua como um extrator de texto simples e formatado, sanitizando e aplicando regras de hifenização nas páginas brutas para visualização justificada no leitor.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `html`, `htm`, `xhtml`, `xhtm`, `mht`, `mhtml`.
* **Codificação de Caracteres:** O extrator detecta automaticamente o charset do arquivo HTML utilizando `ExtUtils.determineEncoding(inputStream)`.
* **Extração de Conteúdo:**
  1. Lê o fluxo do arquivo original a partir da tag `<body>` até `</body>` ou `</html>`.
  2. Caso a hifenização automática (`EbookSettings.isAutoHypens`) esteja ativada, inicializa e aplica as regras linguísticas correspondentes via [HypenUtils](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/foobnix/hypen/HypenUtils.java).
  3. Sanitiza a marcação HTML utilizando o Jsoup com regras seguras (`Safelist.relaxed()`). Se a hifenização não estiver ativa ou sob condições específicas, remove tags de imagens (`removeTags("img")`) para simplificar o fluxo de renderização textual.
  4. Encapsula o texto sanitizado em uma casca HTML limpa: `<html><head></head><body style='text-align:justify;'><br/>$cleanHtml</body></html>`.
  5. Salva o HTML resultante sob o nome de `temp.html` no diretório de saída temporário.

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) usando o nome do arquivo sem extensão como título básico.
* `extractContent`: Salva o HTML justificado em `temp.html` e retorna uma instância de [BookContent.HtmlFile](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookContent.kt).
