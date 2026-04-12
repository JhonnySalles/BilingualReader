# VocabularyActivity

## 🎯 Objetivo / Contexto

Atua como o container principal para as funcionalidades de estudo de vocabulário. Dependendo do contexto de entrada, decide qual fragmento carregar: a visualização geral de vocabulário ou visualizações específicas para um livro ou manga.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [VocabularyActivity.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/vocabulary/VocabularyActivity.kt)
- **Layout XML:** [activity_vocabulary.xml](BilingualReader/src/main/res/layout/activity_vocabulary.xml)
- **Principais Views (IDs):**
  - `toolbar_vocabulary`: Toolbar superior para navegação de retorno.
  - `root_frame_vocabulary`: Container (FrameLayout) onde os fragmentos são trocados.
  - `vocabulary_background_image`: Imagem de fundo dinâmica (capa da obra) que fornece contexto visual ao estudo.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Roteamento Dinâmico:** Utiliza extras do `Intent` (Key: `VOCABULARY_TYPE`) para determinar se deve exibir o `VocabularyFragment` (geral), `VocabularyBookFragment` ou `VocabularyMangaFragment`.
- **Customização de Tema:** Aplica o tema configurado pelo usuário no `onCreate` antes da inflagem do layout.
- **Configuração de Estética Premium:** Gerencia a transparência da barra de status e a visibilidade da imagem de capa com degrade (shadow) para garantir legibilidade e beleza.
- **Estado Estático Compartilhado:** Mantém variáveis de estado estáticas (`VocabularyData`) para persistir preferências de ordenação e filtros durante a navegação entre telas de vocabulário.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** `MainActivity`, `BookReaderFragment`, `MangaReaderFragment`.
- **Próximas Telas (Para onde vai):** `VocabularyFragment`, `VocabularyBookFragment`, `VocabularyMangaFragment`.

## 🌍 Strings / Dicionário (Referência)

- `title_activity_vocabulary`
