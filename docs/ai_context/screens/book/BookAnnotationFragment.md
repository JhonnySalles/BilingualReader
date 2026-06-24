# BookAnnotationFragment

## 🎯 Objetivo / Contexto

Exibe e gerencia as anotações específicas de um livro aberto no leitor. Permite filtrar, buscar e navegar rapidamente para as marcações dentro do contexto da obra atual.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [BookAnnotationFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/book/BookAnnotationFragment.kt)
- **Layout XML:** [fragment_annotation.xml](BilingualReader/src/main/res/layout/fragment_annotation.xml) (Reutilizado do AnnotationFragment geral)
- **ViewModel / Presenter:** [BookAnnotationViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/book/BookAnnotationViewModel.kt)
- **Principais Views (IDs):**
  - `annotation_recycler_view`: Lista de anotações do livro.
  - `annotation_scroll_up` / `annotation_scroll_down`: Navegação rápida por scroll.
  - `annotation_popup_filter`: Menu de filtros (Abas: Filtro, Cor, Capítulos).

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Escopo do Livro:** Diferente do fragmento geral, este exibe apenas as anotações vinculadas ao ID do livro passado via argumentos ou fragmento pai.
- **Navegação via Resultado:** Ao selecionar uma anotação, o fragmento retorna a página selecionada para o `BookReaderFragment` através de um mecanismo de callback ou interface de listener.
- **Ações de Edição:** Permite deletar (via swipe) ou favoritar anotações diretamente da lista.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** `BookReaderFragment` (Através de botão de menu/anotações).
- **Próximas Telas (Para onde vai):** Retorna ao `BookReaderFragment` na página da anotação selecionada.

## 🌍 Strings / Dicionário (Referência)

- `menu_annotation_search`
- `popup_filter_tab_item_color`
- `popup_filter_tab_item_chapters`
- `msg_annotation_delete_confirmation`
