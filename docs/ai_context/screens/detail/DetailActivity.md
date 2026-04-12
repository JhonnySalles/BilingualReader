# DetailActivity

## 🎯 Objetivo / Contexto

A `DetailActivity` serve como um container genérico de tela cheia para exibir os detalhes refinados de uma obra, seja ela um livro ou um mangá. Sua principal responsabilidade é gerenciar o tema da tela, a barra de status e hospedar dinamicamente o fragmento de detalhe correspondente.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [DetailActivity.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/detail/DetailActivity.kt)
- **Layout XML:** [activity_detail.xml](BilingualReader/src/main/res/layout/activity_detail.xml)
- **ViewModel / Presenter:** N/A (Container de Fragments)
- **Principais Views (IDs):**
  - `toolbar_detail`: Barra de ferramentas superior que exibe o nome da obra e o botão de voltar.
  - `root_frame_detail`: FrameLayout que serve como host para a inflagem dos fragmentos `BookDetailFragment` ou `MangaDetailFragment`.
  - `detail_background`: Background persistente da activity que se adapta ao tema selecionado.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Regra 1 (Roteamento Dinâmico):** Ao iniciar, a activity verifica o `Bundle` recebido. Se contiver a chave `MANGA`, carrega o `MangaDetailFragment`. Se contiver a chave `BOOK`, carrega o `BookDetailFragment`. O padrão é o detalhe de Mangá caso nenhuma chave seja encontrada.
- **Regra 2 (Gestão de Temas):** Aplica o tema configurado pelo usuário no processamento do `onCreate` antes de inflar o layout, garantindo consistência visual (Original, Dark, etc.).
- **Regra 3 (Transparência da Status Bar):** Utiliza `ThemeUtil.statusBarTransparentTheme` para garantir que o conteúdo se estenda por trás da barra de status, proporcionando uma experiência imersiva e moderna.
- **Regra 4 (Suporte a Transições):** Implementa `supportFinishAfterTransition` no botão de voltar e no `onBackPressed` para suportar as animações de elementos compartilhados iniciadas na tela de biblioteca.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** [BookLibraryFragment](BilingualReader/docs/ai_context/screens/library/book/BookLibraryFragment.md) ou [MangaLibraryFragment](BilingualReader/docs/ai_context/screens/library/manga/MangaLibraryFragment.md).
- **Próximas Telas (Para onde vai):** Atividades de leitura ou fragmentos de gerenciamento dentro dos detalhes.

## 🌍 Strings / Dicionário (Referência)

- _A Activity herda o comportamento de títulos dos fragmentos hospedados, não possuindo strings fixas de UI próprias além do suporte genérico à Toolbar._
