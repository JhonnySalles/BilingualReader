# MangaLibraryFragment

## 🎯 Objetivo / Contexto

[Uma breve descrição de 1 a 3 frases sobre o que o usuário faz nesta tela].

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [MangaLibraryFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/library/manga/MangaLibraryFragment.kt)
- **Layout XML:** [fragment_manga_library.xml](BilingualReader/src/main/res/layout/fragment_manga_library.xml)
- **ViewModel / Presenter:** [MangaLibraryViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/viewmodel/MangaLibraryViewModel.kt)
- **Principais Views (IDs):**
  - `manga_library_recycler_view`: Lista principal que exibe os mangás da biblioteca.
  - `manga_library_refresh`: SwipeRefreshLayout usado para disparar o escaneamento de novos arquivos de mangá.
  - `manga_library_scroll_up` / `manga_library_scroll_down`: Botões flutuantes para navegação rápida na lista.
  - `manga_library_popup_menu_library`: BottomSheet com abas para configuração de visualização, ordenação e filtros.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Regra 1 (Visualização Customizada):** Oferece diversos modos de exibição: Lista (`LINE`), Grade Pequena/Média/Grande (`GRID_SMALL`, `GRID_MEDIUM`, `GRID_BIG`) e Grade com Separadores por letra inicial (`SEPARATOR_MEDIUM`, `SEPARATOR_BIG`).
- **Regra 2 (Ordenação e Busca):** Ordenação disponível por Nome, Favoritos, Último Acesso, Data, Autor e Gênero. A busca utiliza o padrão de filtros avançados `@` comuns ao app.
- **Regra 3 (Gestão de Scanner):** O `ScannerManga` monitora e atualiza a biblioteca. Se o scanner estiver rodando, o `manga_library_refresh` permanece ativo até o término do processamento.
- **Regra 4 (Importação de Vocabulário):** Funcionalidade exclusiva para mangás que permite importar vocabulário de arquivos externos através de um diálogo de opções (Padrão, Itens Completos, Novos Itens, Re-importar).
- **Regra 5 (Transições Visuais):** Utiliza animações de transição de elementos compartilhados para capa, título e barra de progresso ao navegar para o leitor ou detalhes.
- **Regra 6 (Context Menu):** Permite acesso rápido a: Limpar progresso, Excluir permanentemente, Ver detalhes e Gerenciar Marcadores (bookmarks).

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** Menu Principal (Drawer/NavigationView).
- **Próximas Telas (Para onde vai):** [MangaReaderActivity](BilingualReader/docs/ai_context/screens/reader/manga/MangaReaderActivity.md), [MangaDetailFragment](BilingualReader/docs/ai_context/screens/detail/manga/MangaDetailFragment.md).

## 🌍 Strings / Dicionário (Referência)

- `menu_manga_library_type`
- `menu_manga_library_list_order`
- `menu_manga_library_import_vocab`
- `menu_manga_library_search`
- `vocabulary_import_title`
- `popup_library_manga_tab_item_type`
- `popup_library_manga_tab_item_ordering`
- `popup_library_manga_tab_item_filter`
- `menu_manga_reading_order_change`
- `manga_excluded`
