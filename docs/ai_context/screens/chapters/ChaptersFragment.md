# ChaptersFragment

## 🎯 Objetivo / Contexto

O usuário utiliza esta tela para visualizar a lista de capítulos disponíveis para um livro ou manga específico, permitindo a seleção de um capítulo para leitura imediata ou visualização de progresso.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [ChaptersFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/chapters/ChaptersFragment.kt)
- **Layout XML:** [fragment_chapters.xml](BilingualReader/src/main/res/layout/fragment_chapters.xml)
- **ViewModel / Presenter:** [ChaptersViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/chapters/ChaptersViewModel.kt)
- **Principais Views (IDs):**
  - `chapters_recycler_view`: Lista em grade (Grid) exibindo os capítulos.
  - `chapter_scroll_up`: Botão flutuante para rolar rapidamente para o início da lista.
  - `chapter_scroll_down`: Botão flutuante para rolar rapidamente para o final da lista.
  - `toolbar_chapter`: Barra de ferramentas exibindo o título da obra.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Gestão de Estados de Scroll:** Os botões de scroll (`FloatingActionButton`) são exibidos ou ocultados dinamicamente com base na direção e intensidade da rolagem do `RecyclerView`.
- **Seleção de Capítulo:** Ao clicar em um capítulo, o fragmento comunica a seleção via `NavigatorListener` ou `activityViewModels` para iniciar a leitura.
- **Botões Animados:** Utiliza `AnimatedVectorDrawable` para animações visuais nos botões de scroll ao interagir com eles.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** `BookDetailFragment`, `MangaDetailFragment`.
- **Próximas Telas (Para onde vai):** `BookReaderFragment`, `MangaReaderFragment`.

## 🌍 Strings / Dicionário (Referência)

- `action_scroll_up`
- `action_scroll_down`
