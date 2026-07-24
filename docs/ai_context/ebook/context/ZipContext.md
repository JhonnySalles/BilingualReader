# ZipContext

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [ZipContext](file:///e:/Proj/BilingualReader/ebook/src/main/java/org/ebookdroid/droids/ZipContext.kt) gerencia arquivos compactados no formato **ZIP** que contêm livros. Ele age como um redirecionador dinâmico, identificando e extraindo o e-book de dentro do pacote ZIP e despachando-o para o codec e contexto de renderização apropriado.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Herança:** Estende `PdfContext`.
* **Processamento de Abertura:**
  * No método `openDocumentInner(fileName, password)`:
    1. **Entrada Única Otimizada:** Invoca `CacheZipUtils.isSingleAndSupportEntryFile` para analisar se o arquivo ZIP contém apenas um livro de formato suportado. Caso positivo e o arquivo de cache correspondente já exista, inicializa e redireciona o fluxo de leitura delegando diretamente a chamada para a classe [Fb2Context](file:///e:/Proj/BilingualReader/docs/ai_context/ebook/context/Fb2Context.md).
    2. **Extração Física:** Se não for o caso otimizado, executa `CacheZipUtils.extracIfNeed(fileName, CacheDir.ZipApp)` para extrair fisicamente o conteúdo para a pasta temporária de cache de leitura.
    3. **Prevenção de Loop:** Se o arquivo descompactado resultante ainda for um arquivo `.zip` (nested zip), aborta o processamento retornando `null`.
    4. **Despacho Dinâmico:** Determina qual o codec de contexto correto para o arquivo extraído executando `BookType.getCodecContextByPath(path)`. Dispara a inicialização da renderização delegando para o codec retornado.

## 🗂️ Padrão e Retorno
Retorna o `CodecDocument` inicializado a partir do codec adequado para o arquivo extraído de dentro do ZIP.
