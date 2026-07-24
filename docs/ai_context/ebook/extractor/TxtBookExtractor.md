# TxtBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [TxtBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/TxtBookExtractor.kt) fornece suporte para arquivos de texto plano bruto (**TXT**). Uma vez que arquivos TXT não possuem formatação estruturada ou marcação inerente, o extrator realiza o processamento linha por linha, aplicando detecção de encoding, hifenização e reconstruindo parágrafos HTML baseando-se em preferências do usuário.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `txt`.
* **Codificação:** Utiliza `ExtUtils.determineEncoding(inputStream)` para detectar de forma automática o charset do arquivo de texto original (como UTF-8, Windows-1252, etc.).
* **Processamento de Conteúdo:** O extrator lê o arquivo de texto bruto linha a linha e formata de acordo com as preferências setadas em `EbookSettings`:
  * **Modo PreText (`EbookSettings.isPreText`):**
    * Envolve todo o conteúdo dentro de uma tag `<pre>`.
    * Converte caracteres de tabulação em espaços utilizando o método `retab` com tabstop de 8 espaços.
    * Protege caracteres HTML e transforma linhas iniciadas e finalizadas com caracteres maiúsculos em negrito (`<b>`).
  * **Modo LineBreaks (`EbookSettings.isLineBreaksText`):**
    * Envolve todo o documento em uma única tag `<p>`. Linhas vazias são inseridas como `<br/>`.
  * **Modo Padrão (Parágrafos):**
    * Linhas vazias viram `<br/>`.
    * Linhas em maiúsculo (como títulos) ou que contenham a palavra `"Title:"` são estilizadas em negrito (`<b>`).
    * Linhas padrão são envolvidas individualmente em tags de parágrafo `<p>`.
  * **Hifenização:** Aplica hifenização a cada linha do texto via `HypenUtils.applyHypnes` caso `EbookSettings.isAutoHypens` esteja ativo.
* **Geração de HTML:** Compila o resultado formatado em um arquivo intermediário `txt.html` (ou `pre_txt.html`) na pasta temporária.

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) básico contendo o nome do arquivo.
* `extractContent`: Converte a estrutura plana para HTML estruturado e retorna uma instância de [BookContent.HtmlFile](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookContent.kt).
