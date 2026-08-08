# MobiContext

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [MobiContext](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/MobiContext.kt) faz a ponte de exibição para arquivos da Amazon/Mobipocket (**MOBI**, **AZW**, **AZW3**, **AZW4**) e bancos de dados legados do PalmOS (**PDB**, **PRC**). Ele orquestra a conversão prévia desses formatos binários para EPUB ou HTML intermediário e resolve a paginação através do `MuPdfDocument`.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Herança:** Estende `PdfContext`.
* **Gerenciamento de Caches:**
  * O método `getCacheFileName` calcula um hash a partir do caminho do arquivo original e de preferências de exibição (`isAutoHypens` e `hypenLang`), gerando o arquivo cache `<hash><hash>.epub`.
  * Lida também com arquivos HTML de cache (`<hash><hash>.html`) no caso de decodificações de PalmDOC legados.
  * **Tratamento de PDB:** No início do carregamento, se o formato original for `.pdb`, descarta e exclui qualquer cache `.epub` antigo que possa ter restado corrompido.
* **Processamento de Abertura:**
  * No método `openDocumentInner(fileName, password)`:
    1. Lança `IllegalStateException` caso seja invocado na thread principal de interface gráfica.
    2. Utiliza o [MobiBookExtractor](file:///e:/Proj/BilingualReader/docs/ai_context/ebook/extractor/MobiBookExtractor.md) para extrair o conteúdo do arquivo binário na thread de I/O (`EbookDispatcher.dispatcher`).
    3. **Fluxo EPUB (LibMobi):** Se o extrator retornar uma instância de `BookContent.EpubFile`:
       * Se a hifenização automática (`isAutoHypens`) estiver ativada, chama `EpubBookExtractor.processHyphens` para injetar os hifens no arquivo EPUB final de cache.
    4. **Fluxo HTML (PalmDOC):** Se o extrator retornar `BookContent.HtmlFile` (no caso de arquivos PDB/fallbacks), utiliza o HTML diretamente.
    5. Abre o documento final usando o motor do `MuPdfDocument`.
    6. **Notas de Rodapé:** Se existir o arquivo `.json` de notas no cache, carrega-o. Caso contrário, dispara uma corrotina assíncrona para extrair as notas do EPUB convertido (`EpubBookExtractor.extractFooterNotes`), salvando-as no arquivo JSON e associando-as ao documento.
    7. **Tratamento de Falhas:** Em caso de exceções no processamento ou abertura do documento, exclui o arquivo de cache correspondente para evitar estados inválidos no próximo carregamento.

## 🗂️ Padrão e Retorno
Retorna uma instância de `MuPdfDocument` mapeando o e-book convertido (EPUB ou HTML) com notas de rodapé e hifenização tratadas.
