# MangaReaderFragment

## 🎯 Objetivo / Contexto

O `MangaReaderFragment` é o motor de renderização principal para obras visuais. Ele é responsável por extrair imagens de arquivos comprimidos (CBZ, CBR) ou pastas, gerenciar os modos de visualização (fit width, aspect fit, etc.) e controlar a experiência de transição entre páginas e volumes.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [MangaReaderFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/reader/manga/MangaReaderFragment.kt)
- **Layout XML:** [fragment_manga_reader.xml](BilingualReader/src/main/res/layout/fragment_manga_reader.xml)
- **ViewModel / Presenter:** [MangaReaderViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/viewmodel/MangaReaderViewModel.kt)
- **Principais Views (IDs):**
  - `fragment_manga_reader_pager`: `ImageViewPager` utilizado para leitura paginada com suporte a variados estilos de transição.
  - `fragment_manga_reader_recycler`: `ZoomRecyclerView` utilizado para o modo de leitura contínua (estilo Webtoon).
  - `reader_manga_cover_content`: Camada que exibe a capa e mensagens de status durante o carregamento inicial.
  - `reader_last_page`: Container flutuante que exibe uma miniatura da página anterior ao navegar rapidamente via SeekBar.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Regra 1 (Streaming de Imagens):** Utiliza Picasso com um `MangaHandler` e `Parse` customizados para realizar o streaming de imagens diretamente de dentro de arquivos RAR/ZIP sem a necessidade de extração total prévia, otimizando memória e performance.
- **Regra 2 (Motores de Navegação):** Alterna dinamicamente entre um sistema baseado em `ViewPager` (para navegação Horizontal, Vertical ou RTL com efeitos como "Curl" ou "Stack") e um sistema baseado em `RecyclerView` para rolagem infinita.
- **Regra 3 (Controle de Zoom e Visualização):** Suporta diferentes modos de enquadramento (`FIT_WIDTH`, `ASPECT_FIT`, `ASPECT_FILL`) e permite manter o nível de zoom transacional entre páginas ou habilitar uma lupa de detalhes.
- **Regra 4 (Histórico e Estreia):** Inicia automaticamente um registro de histórico ao abrir a obra e calcula a velocidade média de leitura por página para fornecer estimativas de tempo restante.
- **Regra 5 (Navegação de Busca Visual):** Durante o uso do SeekBar, o fragmento mantém um buffer de bitmaps da "última página" visitada para permitir que o usuário compare rapidamente a posição atual com a anterior.
- **Regra 6 (Conclusão e Continuidade):** Ao detectar o fim de um capítulo ou volume (`hitEnding`), o fragmento consulta a biblioteca para sugerir automaticamente a abertura do próximo volume disponível.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** Hospedado em [MangaReaderActivity](BilingualReader/docs/ai_context/screens/reader/manga/MangaReaderActivity.md).
- **Próximas Telas (Para onde vai):** [MangaDetailFragment](BilingualReader/docs/ai_context/screens/detail/manga/MangaDetailFragment.md) (via fechar) ou o próximo volume da série.

## 🌍 Strings / Dicionário (Referência)

- `reading_manga_open_exception`
- `manga_excluded`
- `file_not_found`
- `reading_manga_pagination_page_curl`
- `reading_manga_scrolling_scrolling`
