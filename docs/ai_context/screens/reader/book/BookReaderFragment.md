# BookReaderFragment

## 🎯 Objetivo / Contexto

O `BookReaderFragment` é o componente central responsável por carregar, renderizar e processar a interação do usuário com documentos PDF ou EPUB. Ele gerencia o ciclo de vida da renderização, o processamento de texto para TTS e a lógica de transição entre páginas.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [BookReaderFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/reader/book/BookReaderFragment.kt)
- **Layout XML:** [fragment_book_reader.xml](BilingualReader/src/main/res/layout/fragment_book_reader.xml)
- **ViewModel / Presenter:** [BookReaderViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/viewmodel/BookReaderViewModel.kt)
- **Principais Views (IDs):**
  - `fragment_book_reader_pager`: `ViewPager2` utilizado para navegação paginada (Horizontal, Vertical, RTL).
  - `fragment_book_reader_recycler`: `ZoomRecyclerView` utilizado para o modo de rolagem contínua (Infinity Scrolling).
  - `reader_book_cover_content`: Tela de carregamento/capa exibida enquanto o documento está sendo processado.
  - `container_book_tts`: Overlay de controle para a funcionalidade de leitura em voz alta.
  - `reader_last_page`: Pequena prévia flutuante da página anterior exibida ao usar a barra de navegação rápida.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Regra 1 (Motores de Renderização):** Suporta dois modos de exibição: `TextViewAdapter` (renderização direta de texto com suporte a formatação rica) e `WebViewAdapter` (baseado em tecnologias web, útil para layouts complexos de EPUB).
- **Regra 2 (Modos de Rolagem e Transição):** Oferece múltiplos estilos de paginação (Default, Curl, Stack, Zoom, Fade, Depth) e a opção de rolagem contínua estilo "Infinity Scrolling".
- **Regra 3 (Dicionário e TTS):** Integra-se com o motor de `Text-to-Speech` (TTS) para leitura assistida, permitindo controle de velocidade, pausa e navegação por sentenças com destaque visual.
- **Regra 4 (Estimativa de Tempo):** Calcula dinamicamente o tempo restante de leitura (`mTimeToEnding`) baseando-se na média de velocidade de leitura do usuário nas páginas atuais.
- **Regra 5 (Sessões de Leitura):** Registra automaticamente o histórico de cada sessão, incluindo o carimbo de tempo inicial/final e a página exata de interrupção para fins de estatística e retomada rápida.
- **Regra 6 (Troca de Obras):** Ao atingir a última página, o fragmento detecta obras relacionadas na biblioteca e oferece um diálogo de confirmação para abrir o próximo volume automaticamente.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** Hospedado em [BookReaderActivity](BilingualReader/docs/ai_context/screens/reader/book/BookReaderActivity.md).
- **Próximas Telas (Para onde vai):** [BookSearchFragment](BilingualReader/docs/ai_context/screens/book/BookSearchFragment.md) ou [AnnotationFragment](BilingualReader/docs/ai_context/screens/annotation/AnnotationFragment.md).

## 🌍 Strings / Dicionário (Referência)

- `reading_book_open_exception`
- `file_not_found`
- `reading_book_close_tts`
- `reading_book_title_position`
- `action_neutral`
