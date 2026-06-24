# VocabularyFragment

## 🎯 Objetivo / Contexto

Lista todo o vocabulário salvo pelo usuário durante a leitura. Oferece ferramentas poderosas para pesquisa, filtragem por favoritos e ordenação por frequência ou ordem alfabética, facilitando o estudo e revisão de termos.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [VocabularyFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/vocabulary/VocabularyFragment.kt)
- **Layout XML:** [fragment_vocabulary.xml](BilingualReader/src/main/res/layout/fragment_vocabulary.xml)
- **ViewModel / Presenter:** [VocabularyViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/vocabulary/VocabularyViewModel.kt)
- **Principais Views (IDs):**
  - `vocabulary_recycler`: Lista paginada exibindo os termos de vocabulário.
  - `vocabulary_refresh`: SwipeRefreshLayout para recarregar a lista.
  - `vocabulary_scroll_up` / `vocabulary_scroll_down`: Botões flutuantes para navegação rápida na lista.
  - `vocabulary_popup_menu_order_filter`: BottomSheet para seleção de critérios de ordenação.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Paginação de Dados:** Utiliza o componente Paging do Android para carregar grandes listas de vocabulário de forma eficiente.
- **Gestão de Favoritos:** Permite marcar/desmarcar termos como favoritos diretamente na lista, com animações síncronas usando `AnimatedVectorDrawable`.
- **Ordenação Flexível:** Suporta ordenação por descrição (A-Z), frequência de uso ou favoritos. A ordenação pode ser alterada via clique longo no ícone do menu.
- **Busca Integrada:** Permite pesquisar termos específicos via `SearchView` na Toolbar. Se acessado via leitor, o termo selecionado pode vir pré-preenchido.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** `VocabularyActivity`.
- **Próximas Telas (Para onde vai):** Detalhes do termo de vocabulário (se implementado).

## 🌍 Strings / Dicionário (Referência)

- `menu_vocabulary_search`
- `config_option_vocabulary_order_description`
- `config_option_vocabulary_order_frequency`
- `config_option_vocabulary_order_favorite`
- `popup_vocabulary_tab_item_ordering`
