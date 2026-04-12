# HistoryFragment

## 🎯 Objetivo / Contexto

[Uma breve descrição de 1 a 3 frases sobre o que o usuário faz nesta tela].

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [HistoryFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/history/HistoryFragment.kt)
- **Layout XML:** [fragment_history.xml](BilingualReader/src/main/res/layout/fragment_history.xml)
- **ViewModel / Presenter:** [HistoryViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/history/HistoryViewModel.kt)
- **Principais Views (IDs):**
  - `history_list`: RecyclerView que exibe a lista de itens do histórico.
  - `history_scroll_up` / `history_scroll_down`: Botões flutuantes para navegação rápida no topo e rodapé da lista.
  - `shimmer_skeleton`: Layout de "shimmer" exibido durante o carregamento inicial.
  - `menu_history_search`: SearchView customizada no menu para busca e filtragem avançada.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Regra 1 (Busca Avançada):** Suporta busca por texto e por filtros especiais usando o prefixo `@` (ex: `@library:`, `@author:`). Oferece sugestões inteligentes enquanto o usuário digita.
- **Regra 2 (Swipe to Delete):** Implementa o padrão de "deslizar para a esquerda" para excluir um item do histórico. Uma confirmação (`MaterialAlertDialogBuilder`) é exibida antes da exclusão permanente.
- **Regra 3 (Context Menu):** Pressionar longamente um item abre um `PopupMenu` com opções para favoritar/desfavoritar, limpar o progresso de leitura, excluir permanentemente ou copiar o nome do arquivo.
- **Regra 4 (Sincronização com Arquivos):** Ao tentar abrir um item, verifica se o arquivo físico ainda existe. Se não existir, marca o item como "excluído" visualmente e avisa o usuário.
- **Regra 5 (Navegação Direta):** Retoma a leitura exatamente do ponto onde o usuário parou (bookmark), disparando a Activity de leitura correspondente (Manga ou Livro).
- **Regra 6 (Feedback de Carregamento):** Utiliza um efeito de Shimmer com esqueletos calculados dinamicamente com base na altura da tela para preencher o espaço enquanto os dados são carregados.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** Menu Principal (Drawer/NavigationView).
- **Próximas Telas (Para onde vai):** [BookReaderActivity](BilingualReader/docs/ai_context/screens/reader/book/BookReaderActivity.md) ou [MangaReaderActivity](BilingualReader/docs/ai_context/screens/reader/manga/MangaReaderActivity.md).

## 🌍 Strings / Dicionário (Referência)

- `history_menu_choice_all`
- `history_manga`
- `history_book`
- `history_delete_description`
- `manga_library_menu_delete`
- `action_delete`
- `manga_excluded`
- `book_excluded`
- `action_scroll_up`
- `action_scroll_down`
