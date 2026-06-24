# MangaDetailFragment

## 🎯 Objetivo / Contexto

Esta tela fornece uma visão detalhada e centralizada de um mangá da biblioteca. O usuário pode conferir metadados técnicos, gerenciar favoritos, visualizar capítulos, arquivos vinculados e metadados avançados (como informações de ComicInfo), além de iniciar a leitura.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [MangaDetailFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/detail/manga/MangaDetailFragment.kt)
- **Layout XML:** [fragment_manga_detail.xml](BilingualReader/src/main/res/layout/fragment_manga_detail.xml)
- **ViewModel / Presenter:** [MangaDetailViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/viewmodel/MangaDetailViewModel.kt)
- **Principais Views (IDs):**
  - `manga_detail_manga_image`: Exibe a capa do mangá, permitindo visualização ampliada via popup.
  - `manga_detail_progress`: Exibe o progresso de leitura em relação ao total de páginas.
  - `manga_detail_chapters_list`: Lista os capítulos disponíveis para seleção e navegação direta.
  - `manga_detail_local_information_comic_info`: Seção de metadados avançados (Story Arches, Personagens, Equipes, Locais).
  - `manga_detail_subtitles_import_vocabulary`: Botão exclusivo para importar vocabulário a partir de legendas vinculadas.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Regra 1 (Processamento de Metadados Locais):** Carrega e exibe informações detalhadas do arquivo como série, volume, editora e autores. Se disponível, processa o padrão `ComicInfo` para exibir personagens e tags específicas.
- **Regra 2 (Importação de Vocabulário):** Permite a extração de termos e sentenças para o dicionário pessoal do usuário utilizando arquivos de legenda (subtitles) vinculados ao mangá.
- **Regra 3 (Interatividade de Imagem):** Utiliza um sistema de zoom e pinch para a capa da obra, com atualização assíncrona da imagem em alta resolução via `MangaImageCoverController`.
- **Regra 4 (Navegação por Capítulos):** Mapeia a seleção de uma pasta/capítulo para a página correspondente e inicia a `MangaReaderActivity` já posicionada corretamente.
- **Regra 5 (Persistência de Marcadores):** Gerencia marcadores de página personalizados (Bookmarks) que podem ser acessados através de um menu de popup rápido.
- **Regra 6 (Integração Web):** Busca e sincroniza informações de serviços externos (MAL, etc.), preenchendo sinopses e dados de status de publicação automaticamente.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** [MangaLibraryFragment](BilingualReader/docs/ai_context/screens/library/manga/MangaLibraryFragment.md).
- **Próximas Telas (Para onde vai):** [MangaReaderActivity](BilingualReader/docs/ai_context/screens/reader/manga/MangaReaderActivity.md), [VocabularyActivity](BilingualReader/docs/ai_context/screens/vocabulary/VocabularyActivity.md).

## 🌍 Strings / Dicionário (Referência)

- `manga_detail_manga_deleted`
- `manga_library_menu_delete`
- `manga_detail_local_information_authors`
- `manga_detail_local_information_series`
- `manga_detail_local_information_volume`
- `manga_detail_local_information_publisher`
- `manga_detail_local_information_release`
