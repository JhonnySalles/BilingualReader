# VocabularyBookFragment

## 🎯 Objetivo / Contexto

Permite ao usuário estudar o vocabulário específico de um livro. Esta tela foca nos termos encontrados na obra selecionada, permitindo buscas refinadas pelo título do livro e gestão de aprendizado focada.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [VocabularyBookFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/vocabulary/book/VocabularyBookFragment.kt)
- **Layout XML:** [fragment_vocabulary_book.xml](BilingualReader/src/main/res/layout/fragment_vocabulary_book.xml)
- **ViewModel / Presenter:** [VocabularyBookViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/vocabulary/book/VocabularyBookViewModel.kt)
- **Principais Views (IDs):**
  - `vocabulary_book_recycler`: RecyclerView para a lista de vocabulário do livro.
  - `vocabulary_book_edittext`: Campo de texto editável para o nome do livro (usado como filtro).
  - `vocabulary_book_refresh`: SwipeRefreshLayout para atualização manual.
  - `vocabulary_book_scroll_up/down`: Botões flutuantes para rolagem rápida.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Filtro por Título:** Diferente do fragmento geral, este possui um `TextInputEditText` que monitora mudanças de texto (`addTextChangedListener`) para filtrar o vocabulário por título da obra com um delay de 1 segundo (debounce).
- **Escopo de Obra:** Carrega inicialmente os termos vinculados ao objeto `Book` definido no fragmento através do método `setObject`.
- **Persistência de Ordenação:** Salva o critério de ordenação selecionado diretamente no `SharedPreferences` do usuário (Key: `BOOK_ORDER`).
- **Animações de Interface:** Utiliza animações de entrada e saída para os botões de scroll e transições no BottomSheet de ordenação.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** `VocabularyActivity`.
- **Próximas Telas (Para onde vai):** Retorno ou detalhes do termo.

## 🌍 Strings / Dicionário (Referência)

- `vocabulary_book`
- `action_scroll_up`
- `action_scroll_down`
- `menu_vocabulary_search`
