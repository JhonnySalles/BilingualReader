# BookLibraryFragment

## 🎯 Objetivo / Contexto

[Uma breve descrição de 1 a 3 frases sobre o que o usuário faz nesta tela].

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [BookLibraryFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/library/book/BookLibraryFragment.kt)
- **Layout XML:** [fragment_book_library.xml](BilingualReader/src/main/res/layout/fragment_book_library.xml)
- **ViewModel / Presenter:** [BookLibraryViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/viewmodel/BookLibraryViewModel.kt)
- **Principais Views (IDs):**
  - `book_library_recycler_view`: Lista principal que exibe os livros da biblioteca.
  - `book_library_refresh`: SwipeRefreshLayout usado para disparar o escaneamento de novos arquivos.
  - `book_library_scroll_up` / `book_library_scroll_down`: Botões flutuantes para retornar ao topo ou ir ao final da lista.
  - `book_library_popup_menu_library`: BottomSheet que contém as opções de filtro, ordenação e tipo de visualização.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Regra 1 (Visualização Dinâmica):** Suporta múltiplos modos de exibição: Lista (`LINE`), Grade (`GRID_BIG`, `GRID_MEDIUM`) e Grade com Separadores (`SEPARATOR_BIG`, `SEPARATOR_MEDIUM`). A alteração do layout persiste nas configurações.
- **Regra 2 (Ordenação e Filtragem):** Permite ordenar livros por Nome, Último Acesso, Data de Criação, Favoritos, Autor, Gênero e Série. A busca suporta filtros avançados via prefixo `@`.
- **Regra 3 (Escaneamento de Arquivos):** Integra-se com o `ScannerBook` para atualizar a biblioteca local. O progresso é exibido através de notificações e refletido em tempo real na lista via `UpdateHandler`.
- **Regra 4 (Gestão de Favoritos e Progresso):** O usuário pode marcar livros como favoritos e limpar o progresso de leitura (history/bookmark) através de menus de contexto.
- **Regra 5 (Transições de UI):** Implementa animações de transição compartilhada (`ActivityOptions.makeSceneTransitionAnimation`) ao abrir os detalhes ou o leitor de livros, preservando o contexto visual da capa e título.
- **Regra 6 (Persistência de Estado):** Utiliza um sistema de pilha (`Stack`) no ViewModel para gerenciar o estado da biblioteca entre diferentes níveis de navegação ou rotações de tela.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** Menu Principal (Drawer/NavigationView).
- **Próximas Telas (Para onde vai):** [BookReaderActivity](BilingualReader/docs/ai_context/screens/reader/book/BookReaderActivity.md), [BookDetailFragment](BilingualReader/docs/ai_context/screens/detail/book/BookDetailFragment.md).

## 🌍 Strings / Dicionário (Referência)

- `menu_book_library_type`
- `menu_book_library_list_order`
- `menu_book_library_search`
- `popup_library_book_tab_item_type`
- `popup_library_book_tab_item_ordering`
- `popup_library_book_tab_item_filter`
- `menu_reading_book_order_change`
- `book_excluded`
- `file_not_found`
