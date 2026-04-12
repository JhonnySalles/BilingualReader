# BookReaderActivity

## 🎯 Objetivo / Contexto

A `BookReaderActivity` é a tela principal de imersão para leitura de livros (PDF/EPUB). Ela provê a interface (HUD) necessária para navegação, configuração e acompanhamento do progresso, enquanto delega a renderização do conteúdo ao `BookReaderFragment`.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [BookReaderActivity.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/reader/book/BookReaderActivity.kt)
- **Layout XML:** [activity_book_reader.xml](BilingualReader/src/main/res/layout/activity_book_reader.xml)
- **ViewModel / Presenter:** [BookReaderViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/viewmodel/BookReaderViewModel.kt)
- **Principais Views (IDs):**
  - `toolbar_book_reader`: Barra de ferramentas superior com acesso a capítulos, busca e configurações.
  - `reader_book_bottom_progress`: Barra de busca pontilhada personalizada (`DottedSeekBar`) para navegação rápida entre páginas.
  - `popup_book_configuration_bottom_sheet`: Menu inferior (ou lateral) com abas para ajuste de Fonte, Layout e Idioma.
  - `reader_book_container_touch_demonstration`: Camada de sobreposição que demonstra visualmente as áreas de toque configuradas.
  - `container_book_progress`: Overlay de carregamento que exibe progresso, relógio e título da obra.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Regra 1 (Navegação por Toque):** Implementa um sistema de zonas de toque customizáveis (Top, Bottom, Corners, Sides) que executam ações como trocar página, abrir capítulos ou marcar página. O mapeamento é visualizado através de uma animação temporária (`openTouchFunctions`).
- **Regra 2 (Índice de Páginas):** Gera dinamicamente um diálogo de índice baseado na estrutura interna (`Outline`) do documento PDF/EPUB. Se o documento não possuir índice, exibe um alerta de "Índice vazio".
- **Regra 3 (Configurações em Abas):** Utiliza um sistema de popups (`PopupBookFont`, `PopupBookLayout`, `PopupBookLanguage`) integrados em um `ViewPager` dentro de um `BottomSheet`. Permite configurar tamanho de fonte, estilo de rolagem (Paginação vs Scroll) e idiomas de tradução.
- **Regra 4 (Sincronização de Progresso):** Atualiza em tempo real o título da toolbar inferior com a página atual, total de páginas e porcentagem de leitura calculada.
- **Regra 5 (Integração de Sistema):** Suporta abertura de arquivos externos via `Intent.ACTION_VIEW`, permitindo que o app atue como leitor padrão de documentos do dispositivo.
- **Regra 6 (HUD Inteligente):** O HUD (toolbars superior e inferior) é ocultado/exibido pelo fragmento de leitura para maximizar a área útil; a Activity gerencia as animações de entrada e saída dessas barras.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** [BookDetailFragment](BilingualReader/docs/ai_context/screens/detail/book/BookDetailFragment.md), [HistoryFragment](BilingualReader/docs/ai_context/screens/history/HistoryFragment.md) ou "Abrir com" do sistema.
- **Próximas Telas (Para onde vai):** [ChaptersFragment](BilingualReader/docs/ai_context/screens/chapters/ChaptersFragment.md) (para seleção de capítulos remotos) ou fechar para retornar à biblioteca.

## 🌍 Strings / Dicionário (Referência)

- `reading_book_title_position`
- `reading_book_title_chapter`
- `reading_book_page_index`
- `reading_book_page_empty`
- `popup_reading_book_tab_item_font`
- `popup_reading_book_tab_item_layout`
- `popup_reading_book_tab_item_language`
