# VocabularyMangaFragment

## 🎯 Objetivo / Contexto

Permite ao usuário estudar o vocabulário específico de um manga. É focado na gestão dos termos salvos durante a leitura de obras visuais, oferecendo filtros por título e sincronização com o progresso de leitura.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** [VocabularyMangaFragment.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/vocabulary/manga/VocabularyMangaFragment.kt)
- **Layout XML:** [fragment_vocabulary_manga.xml](BilingualReader/src/main/res/layout/fragment_vocabulary_manga.xml)
- **ViewModel / Presenter:** [VocabularyMangaViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/vocabulary/manga/VocabularyMangaViewModel.kt)
- **Principais Views (IDs):**
  - `vocabulary_manga_recycler`: RecyclerView para a lista de vocabulário do manga.
  - `vocabulary_manga_edittext`: Campo de busca rápida pelo nome do manga.
  - `vocabulary_manga_refresh`: Componente para atualização da lista.
  - `vocabulary_manga_scroll_up/down`: Botões flutuantes para navegação rápida.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Filtro Assíncrono:** Implementa busca por título utilizando um `TextWatcher` com mecanismo de debounce (atraso de 1 segundo) para evitar requisições excessivas ao banco de dados enquanto o usuário digita.
- **Escopo do Manga:** Carrega apenas os vocabulários associados ao ID do manga ativo na sessão.
- **Preferências de Estudo:** Salva a última ordem de visualização utilizada (Key: `MANGA_ORDER`) para consistência entre sessões de estudo.
- **Interação Visual:** Utiliza `AnimatedVectorDrawable` para feedbacks visuais ao favoritar termos e animações de elevação (Z-axis) nos botões de navegação lateral.

## 🔄 Fluxo de Navegação

- **Telas Anteriores (De onde vem):** `VocabularyActivity`.
- **Próximas Telas (Para onde vai):** Detalhes do vocabulário ou visualização ampliada do card.

## 🌍 Strings / Dicionário (Referência)

- `vocabulary_manga`
- `action_scroll_up`
- `action_scroll_down`
- `menu_vocabulary_search`
