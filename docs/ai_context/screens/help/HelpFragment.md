# HelpFragment

## 🎯 Objetivo / Contexto

- **Classe Principal:** [HelpFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/help/HelpFragment.kt)
- **Layout XML:** [fragment_help.xml](BilingualReader/src/main/res/layout/fragment_help.xml)
- **ViewModel / Presenter:** N/A (Componente estático de documentação)
- **Principais Views (IDs):**
  - `help_scroll_view`: View de rolagem principal que contém o tutorial.
  - `help_scroll_up`: Botão flutuante (FAB) para retornar rapidamente ao topo da tela.
  - IDs de âncoras (ex: `help_library_title`, `help_reader_title`, etc.): Usados como pontos de destino para a navegação interna.
  - IDs de menus (ex: `help_library_content`, `help_reader_content`, etc.): Itens do sumário que acionam o scroll suave.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Regra 1 (Navegação Interna):** A tela possui um sumário no topo. Ao clicar em um tópico (ex: `help_library_content`), o `ScrollView` realiza um `smoothScrollTo` para a posição do título correspondente.
- **Regra 2 (Botão "Voltar ao Topo"):** O botão flutuante `help_scroll_up` é exibido apenas quando o usuário realiza um scroll descendente significativo (mais de 150 pixels) e é ocultado automaticamente após 3 segundos ou ao realizar scroll ascendente.
- **Regra 3 (Feedback Visual):** O botão de scroll utiliza um `AnimatedVectorDrawable` para prover animação ao ser clicado.
- **Regra 4 (Gerenciamento de Recursos):** Utiliza um `Handler` para gerenciar o timer de ocultação do botão, garantindo a limpeza dos callbacks no `onDestroy`.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** Menu Principal (Drawer/NavigationView).
- **Próximas Telas (Para onde vai):** Navegação interna (scroll entre tópicos).

## 🌍 Strings / Dicionário (Referência)

- `help_content`
- `help_library_content` / `help_library_title` / `help_library_description_*`
- `help_reader_content` / `help_reader_title` / `help_reader_description_*`
- `help_reader_manga_content` / `help_reader_manga_title` / `help_reader_manga_*`
- `help_reader_book_content` / `help_reader_book_title` / `help_reader_book_*`
- `help_subtitle_content` / `help_subtitle_title`
- `help_vocabulary_content` / `help_vocabulary_title`
- `help_kanji_content` / `help_kanjis_title`
- `help_floating_popup_content` / `help_floating_popup_title`
- `help_languages_support_content` / `help_language_support_title`
- `help_statistics_content` / `help_statistics_title`
- `help_themes_content` / `help_themes_title`
- `help_share_content` / `help_share_title`

