# PmlzBookExtractor

🤖 **Applying knowledge of `@[documentation-writer]`...**

## 🎯 Objetivo / Contexto
O [PmlzBookExtractor](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/extractor/PmlzBookExtractor.kt) gerencia arquivos compactados do Palm Markup Language no formato **PMLZ**. O formato PMLZ é um arquivo ZIP que contém um documento de texto com sintaxe especial do Palm Reader (`.pml`) e uma pasta de imagens associadas.

## ⚙️ Regras de Negócio e Lógica (Core Logic)
* **Formatos Suportados:** `pmlz`.
* **Imagem de Capa:** Abre o ZIP e busca por arquivos que terminem com `cover.jpg/png/jpeg` ou adota a primeira imagem encontrada dentro do ZIP.
* **Extração de Conteúdo:**
  1. Cria uma pasta de destino `pmlz_temp_<hash>`.
  2. Descompacta todas as entradas do ZIP para este diretório e localiza o arquivo `.pml`. Se não existir, lança uma exceção `IOException`.
  3. Lê o texto do arquivo `.pml` com codificação `cp1252` (Windows-1252).
  4. Realiza o parse da marcação PML customizada e a converte em HTML padrão via método `pmlToHtml`:
     * **Entidades de caracteres (`\aXXX`):** Substitui por códigos de caracteres equivalentes da codificação `cp1252`.
     * **Imagens (`\m="filename.png"`):** Converte para `<img src="pmlBaseName_img/filename.png" style="max-width:100%;" /><br/>`.
     * **Negrito (`\B`):** Comutador que alterna entre abrir `<b>` e fechar `</b>`.
     * **Itálico (`\x`):** Comutador que alterna entre abrir `<i>` e fechar `</i>`.
     * **Alinhamento Central (`\c`):** Alterna abrindo `<div style="text-align:center;">` e fechar `</div>`.
     * **Âncoras (`\Q="anchor"`):** Converte para `<a name="anchor"></a>`.
     * **Hyperlinks (`\q="#link"`):** Abre `<a href="#link">` e fecha no próximo `\q`.
     * **Comandos Descartados:** Remove outros comandos com barra invertida (ex: `\T`, `\fn`).
     * **Quebras de Linha:** Substitui `\n` por `<br/>`.
  5. Salva o HTML resultante como `index.html` no mesmo diretório.

## 🗂️ Padrão e Retorno
* `extractMetadata`: Retorna [BookMetadata](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookMetadata.kt) básico usando o nome do arquivo sem extensão.
* `extractContent`: Converte a sintaxe PML em HTML e retorna um [BookContent.HtmlFile](file:///e:/Proj/BilingualReader/ebook/src/main/java/br/com/ebook/core/BookContent.kt) apontando para o arquivo `index.html` gerado.
