# MangaReaderActivity

## 🎯 Objetivo / Contexto

A `MangaReaderActivity` é a interface de alto nível para leitura de mangás e quadrinhos (CBZ, CBR, Pastas). Ela gerencia o ecossistema de ferramentas auxiliares (OCR, Legendas, Filtros de Cores) e a navegação entre volumes, enquanto hospeda o `MangaReaderFragment` para a renderização das imagens.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [MangaReaderActivity.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/reader/manga/MangaReaderActivity.kt)
- **Layout XML:** [activity_manga_reader.xml](BilingualReader/src/main/res/layout/activity_manga_reader.xml)
- **ViewModel / Presenter:** [MangaReaderViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/viewmodel/MangaReaderViewModel.kt)
- **Principais Views (IDs):**
  - `reader_manga_toolbar_reader`: Barra superior com título da obra e acesso a menus de OCR e capítulos.
  - `reader_manga_bottom_progress`: Barra de progresso pontilhada (`DottedSeekBar`) para salto rápido de páginas.
  - `reader_manga_container_clock_battery`: Overlay discreto que exibe a hora e o nível de bateria atual.
  - `popup_manga_translate_bottom_sheet`: Menu modular para gerenciamento de legendas externas e dicionários.
  - `reader_manga_container_chapters_list`: Painel de acesso rápido aos outros volumes da mesma série na biblioteca.
  - `reader_manga_container_touch_demonstration`: Camada de ajuda visual que demonstra as áreas de toque configuradas.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Regra 1 (Integração OCR):** Permite a execução de reconhecimento óptico de caracteres (OCR) na página atual. O usuário pode alternar o idioma do OCR dinamicamente via `mLanguageOcrDescription` para melhorar a precisão em obras bilíngues.
- **Regra 2 (Ecossistema de Legendas):** Gerencia legendas externas através do `SubTitleController`. Oferece leitura flutuante (`FloatingSubtitleReader`) sincronizada com o progresso da página e ferramentas de vinculação manual (`PagesLinkActivity`).
- **Regra 3 (Processamento de Imagem):** Possui filtros de cor e brilho customizáveis (`PopupMangaColorFilterFragment`) para melhorar a legibilidade de scans com baixa qualidade ou fundos escuros.
- **Regra 4 (Navegação em Série):** Implementa lógica de "Próximo/Anterior" (`switchManga`) que busca automaticamente o próximo volume lógico na biblioteca, facilitando maratonas de leitura.
- **Regra 5 (Atalhos Dinâmicos):** Cria atalhos (`Dynamic Shortcuts`) no launcher do Android para as duas últimas obras lidas, utilizando capas adaptativas geradas em tempo real.
- **Regra 6 (Imersão e Status):** Configura o modo imersivo (Fullscreen) e mantém a tela ligada (`FLAG_KEEP_SCREEN_ON`) durante a leitura, além de prover monitoramento em tempo real do status da bateria.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** [MangaDetailFragment](BilingualReader/docs/ai_context/screens/detail/manga/MangaDetailFragment.md), Biblioteca ou Atalhos do Sistema.
- **Próximas Telas (Para onde vai):** [MangaReaderFragment](BilingualReader/docs/ai_context/screens/reader/manga/MangaReaderFragment.md), [PagesLinkActivity](BilingualReader/docs/ai_context/screens/pages_link/PagesLinkActivity.md) ou Diálogo de Seleção de OCR.

## 🌍 Strings / Dicionário (Referência)

- `switch_next_comic`
- `switch_next_comic_not_found`
- `languages_description`
- `reading_manga_page_index`
- `popup_reading_manga_tab_item_subtitle`
- `popup_reading_manga_tab_item_configuration_brightness`
