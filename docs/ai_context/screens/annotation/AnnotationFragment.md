# AnnotationFragment

## 🎯 Objetivo / Contexto

Permite ao usuário gerenciar todas as anotações criadas durante a leitura de livros e mangas. Oferece ferramentas de busca, filtragem por tipo e cor, e navegação direta para o ponto da obra onde a anotação foi feita.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [AnnotationFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/annotation/AnnotationFragment.kt)
- **Layout XML:** [fragment_annotation.xml](BilingualReader/src/main/res/layout/fragment_annotation.xml)
- **ViewModel / Presenter:** [AnnotationViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/annotation/AnnotationViewModel.kt)
- **Principais Views (IDs):**
  - `annotation_recycler_view`: Lista de anotações exibida com o `AnnotationLineAdapter`.
  - `annotation_scroll_up` / `annotation_scroll_down`: Botões flutuantes para navegação rápida na lista.
  - `annotation_popup_filter`: BottomSheet para aplicar filtros avançados.
  - `annotation_popup_filter_tab`: Abas dentro do filtro (Tipo, Cor, Capítulos).

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Filtragem Multinível:** Permite filtrar anotações por tipo (Original/Traduzido), por cor da marcação e por capítulos específicos da obra.
- **Ações Rápidas:** Suporta deslizar (swipe) para excluir anotações e clique longo para favoritar ou alterar a cor da anotação.
- **Busca em Tempo Real:** Integração com o `SearchView` no menu superior para filtrar a lista de anotações por texto.
- **Navegação de Retorno:** Ao clicar em uma anotação, o aplicativo navega de volta para o leitor correspondente, abrindo na página exata da marcação.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** `MainActivity` (Gaveta de navegação).
- **Próximas Telas (Para onde vai):** `BookReaderFragment`, `MangaReaderFragment`.

## 🌍 Strings / Dicionário (Referência)

- `menu_annotation_search`
- `popup_filter_tab_item_color`
- `popup_filter_tab_item_chapters`
- `popup_filter_tab_item_filter`
- `msg_annotation_delete_confirmation`
