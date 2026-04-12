# BookDetailFragment

## 🎯 Objetivo / Contexto

O usuário utiliza esta tela para visualizar informações completas de um livro, gerenciar seus metadados (tags, favoritos), acompanhar o progresso de leitura e acessar capítulos ou arquivos vinculados. É o hub central de gerenciamento de uma obra específica da biblioteca de livros.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [BookDetailFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/detail/book/BookDetailFragment.kt)
- **Layout XML:** [fragment_book_detail.xml](BilingualReader/src/main/res/layout/fragment_book_detail.xml)
- **ViewModel / Presenter:** [BookDetailViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/viewmodel/BookDetailViewModel.kt)
- **Principais Views (IDs):**
  - `book_detail_book_image`: Exibe a capa do livro com suporte a zoom ao clicar.
  - `book_detail_progress`: Barra de progresso visual do status de leitura.
  - `book_detail_chapters_list`: Lista de capítulos ou marcadores internos do documento.
  - `book_detail_information`: Seção contendo sinopse, autores, editora e outros metadados locais.
  - `book_detail_buttons`: Painel de ações rápidas (Favoritar, Marcar como lido, Gerenciar Tags, Vocabulário, Excluir).

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Regra 1 (Dinâmica de Cores):** A tela adapta as cores de destaque e da barra de status baseando-se na paleta de cores da imagem de capa, utilizando `ColorUtil.isDarkColor`.
- **Regra 2 (Gestão de Capas):** Permite visualizar a capa em tela cheia com gestos de pinch-to-zoom e atualiza a capa de forma assíncrona via `BookImageCoverController`.
- **Regra 3 (Exclusão Segura):** A funcionalidade de exclusão solicita confirmação e realiza a remoção tanto do registro no banco de dados quanto do arquivo físico no armazenamento.
- **Regra 4 (Sincronização com o Leitor):** Ao selecionar um capítulo na lista, o progresso do livro é atualizado e o usuário é redirecionado para a `BookReaderActivity` na posição exata.
- **Regra 5 (Metadados Externos):** Exibe informações capturadas da web (WebInformation), incluindo títulos alternativos, status de publicação e obras relacionadas.
- **Regra 6 (Gerenciamento de Idioma):** Permite alterar o idioma associado ao livro através de um menu dropdown, o que impacta o processamento futuro de vocabulário e tradução.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** [BookLibraryFragment](BilingualReader/docs/ai_context/screens/library/book/BookLibraryFragment.md) (via transição de elementos compartilhados).
- **Próximas Telas (Para onde vai):** [BookReaderActivity](BilingualReader/docs/ai_context/screens/reader/book/BookReaderActivity.md), [VocabularyActivity](BilingualReader/docs/ai_context/screens/vocabulary/VocabularyActivity.md).

## 🌍 Strings / Dicionário (Referência)

- `book_detail_book_deleted`
- `book_library_menu_delete`
- `book_library_menu_delete_description`
- `book_detail_information_synopsis`
- `book_detail_information_publisher`
- `book_detail_information_author`
- `book_detail_information_isbn`
