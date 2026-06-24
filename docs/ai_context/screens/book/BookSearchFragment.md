# BookSearchFragment

## 🎯 Objetivo / Contexto

O usuário utiliza esta tela para realizar pesquisas de texto integral dentro do livro atualmente aberto. A tela exibe resultados contextuais e permite navegar rapidamente para a posição exata do texto no leitor.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [BookSearchFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/book/BookSearchFragment.kt)
- **Layout XML:** [fragment_book_search.xml](BilingualReader/src/main/res/layout/fragment_book_search.xml)
- **ViewModel / Presenter:** [BookSearchViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/book/BookSearchViewModel.kt)
- **Principais Views (IDs):**
  - `book_search_recycler_view`: Lista de resultados encontrados no documento.
  - `book_search_history_list`: Histórico de termos pesquisados anteriormente.
  - `book_search_in_progress`: Indicador de progresso circular enquanto o parser varre o documento.
  - `book_search_stop`: Botão de interrupção para cancelar uma busca em andamento.
  - `book_search_history_clear`: Botão para limpar todo o histórico de buscas.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Regra 1 (Parsing do Documento):** Utiliza um `DocumentParse` compartilhado para realizar a leitura e busca textual dentro dos arquivos (PDF, EPUB, etc.). A busca é assíncrona para não travar a UI.
- **Regra 2 (Histórico Persistente):** Mantém um histórico de buscas realizadas, permitindo repetir uma pesquisa rapidamente ao clicar em um item da lista de histórico.
- **Regra 3 (Contexto da Busca):** Os resultados exibidos na `RecyclerView` mostram o trecho do texto onde o termo foi encontrado, facilitando a identificação pelo usuário.
- **Regra 4 (Integração com Anotações):** Através de um pressionamento longo em um resultado de busca, o usuário pode criar uma anotação (`BookAnnotation`) automaticamente com o texto encontrado.
- **Regra 5 (Navegação de Retorno):** Ao selecionar um resultado, o fragmento envia as informações de posição de volta para a Activity de leitura através de um `Bundle` e encerra sua execução via `onBack()`.
- **Regra 6 (Controle de Pesquisa):** Permite interromper buscas longas em documentos grandes através do botão `stop`.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** [BookReaderActivity](BilingualReader/docs/ai_context/screens/reader/book/BookReaderActivity.md).
- **Próximas Telas (Para onde vai):** Retorna para [BookReaderActivity](BilingualReader/docs/ai_context/screens/reader/book/BookReaderActivity.md) com o resultado selecionado.

## 🌍 Strings / Dicionário (Referência)

- `book_search_history_title`
- `book_search_history_clear`
- `book_search_stop`
- `menu_item_book_search_add_annotation`
- `action_scroll_up`
- `action_scroll_down`
