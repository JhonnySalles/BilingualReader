# PopupMangaColorFilterFragment

## 🎯 Objetivo / Contexto

Fornece controles deslizantes e seletores para aplicar filtros de cor em tempo real sobre as páginas do manga. Permite ajustar brilho, contraste (via cores), escala de cinza, inversão de cores e filtro de luz azul para melhorar a experiência de leitura.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [PopupMangaColorFilterFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/reader/manga/PopupMangaColorFilterFragment.kt)
- **Layout XML:** [popup_manga_color_filter.xml](BilingualReader/src/main/res/layout/popup_manga_color_filter.xml)
- **ViewModel / Presenter:** [MangaReaderViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/reader/manga/MangaReaderViewModel.kt)
- **Principais Views (IDs):**
  - `popup_manga_switch_color_filter`: Ativa/Desativa o filtro de cor customizado.
  - `popup_manga_seekbar_color_filter_red/green/blue/alpha`: Sliders para ajustar os componentes RGBA do filtro.
  - `popup_manga_switch_blue_light`: Ativa o filtro de luz azul (descanso ocular).
  - `popup_manga_switch_grayscale`: Alterna para o modo escala de cinza.
  - `popup_manga_switch_invert_color`: Inverte as cores da página.
  - `popup_manga_switch_sepia_color`: Aplica o tom de sépia.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Atualização Reativa:** As mudanças nos `SeekBar` invocam métodos do `ViewModel` (ex: `changeColorsFilter`), que por sua vez atualizam LiveDatas observados pelo leitor principal para reaplicar o `ColorMatrix` nas imagens.
- **Persistência Visual:** Os valores dos filtros são mantidos durante a sessão de leitura através do `ViewModel`.
- **Cálculo de Porcentagem:** Para o filtro de luz azul, o valor do slider (0-200) é convertido para uma string de porcentagem (0-100%) para exibição ao usuário.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** `MangaReaderFragment` (Menu de configurações visuais).
- **Próximas Telas (Para onde vai):** Permanece na tela de leitura, aplicando os efeitos visualmente.

## 🌍 Strings / Dicionário (Referência)

- `popup_reading_manga_custom_color_filter`
- `popup_reading_manga_blue_light`
- `popup_reading_manga_grayscale`
- `popup_reading_manga_invert_color`
- `popup_reading_manga_sepia`
- `color_filter_r_value` / `g_value` / `b_value` / `a_value`
