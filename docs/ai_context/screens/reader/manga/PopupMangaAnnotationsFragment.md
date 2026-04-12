# PopupMangaAnnotationsFragment

## 🎯 Objetivo / Contexto

Exibe uma lista rápida de anotações feitas no manga atual em formato de grid. Permite ao leitor saltar rapidamente para a página de uma anotação ou excluí-la sem sair da tela de leitura principal.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [PopupMangaAnnotationsFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/reader/manga/PopupMangaAnnotationsFragment.kt)
- **Layout XML:** [popup_manga_annotations.xml](BilingualReader/src/main/res/layout/popup_manga_annotations.xml)
- **ViewModel / Presenter:** [MangaReaderViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/reader/manga/MangaReaderViewModel.kt) (Compartilhado via activityViewModels)
- **Principais Views (IDs):**
  - `popup_manga_annotations_list`: RecyclerView exibindo as anotações em `StaggeredGridLayoutManager`.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Comunicação em Tempo Real:** Utiliza o `MangaReaderViewModel` compartilhado para observar mudanças nas anotações e refletir deletações imediatamente.
- **Navegação de Página:** Ao clicar em um card de anotação, o fragmento invoca `mListener?.setCurrentPage(annotation.page)` para mudar a página do leitor de manga principal.
- **Gestão de Layout Dinâmico:** O número de colunas no grid de anotações é calculado dinamicamente com base na largura da tela disponível (`R.dimen.manga_annotation_layout_width`).
- **Exclusão via Swipe:** Implementa `ItemTouchHelper` para permitir que o usuário delete anotações deslizando para a esquerda ou direita.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** `MangaReaderFragment` (Através de menu popup ou botão de anotações).
- **Próximas Telas (Para onde vai):** Permanece no `MangaReaderFragment`, alterando a página atual se uma anotação for clicada.

## 🌍 Strings / Dicionário (Referência)

- `popup_reading_manga_open`
- `action_scroll_up`
- `action_scroll_down`
