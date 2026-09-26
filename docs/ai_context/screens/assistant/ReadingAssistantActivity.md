# ReadingAssistantActivity

## Objetivo

Tela de perguntas via LLM (on-device MediaPipe ou OpenRouter) para livro e mangá. O usuário escolhe a base de contexto e pergunta sobre a obra em leitura.

## Arquivos

- [ReadingAssistantActivity.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/assistant/ReadingAssistantActivity.kt)
- [ReadingAssistantViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/assistant/ReadingAssistantViewModel.kt)
- [AssistantSelectionHelper.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/assistant/AssistantSelectionHelper.kt)
- [ReadingSummaryDialog.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/assistant/ReadingSummaryDialog.kt)
- [LlmSettings.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/util/helpers/LlmSettings.kt)
- Providers: `BookContextProvider`, `MangaContextProvider` (+ `OcrPageCache`)

## UI

- Empty state (título, hint, chips) fica **acima** do campo de enviar, não no centro da lista.
- Toolbar: `MenuUtil.tintToolbar` antes de `setSupportActionBar` / `setDisplayHomeAsUpEnabled` (padrão Vocabulary/Menu); ícones do menu com `toolbarTitleAccents` (mesma cor dos acentos da toolbar).

## Seleção de contexto

| Tipo | Opções | Default | Limite (Config) |
|------|--------|---------|-----------------|
| Livro | Capítulos do outline | últimos N (`DEFAULT_BOOK_CHAPTERS`) | `MAX_BOOK_CHAPTERS` (1–20) |
| Mangá | Páginas | `current ± radius` | `MAX_MANGA_PAGES` (1–50) |

- Seleção persistida em prefs: `LLM_ASSISTANT_SELECTION_{TYPE}_{id}` (`c:…` / `p:…`).
- Mangá: botão **Intervalo** no multi-choice (From/To, 1-based).
- Campo do seletor mostra contagem (ex. `3 capítulos · …`, `5 pages · Págs. 12–16`).
- OCR de páginas cacheado em memória + `filesDir/llm/ocr_cache/`.

## Config AI relevante

- Max context chars, max capítulos, max páginas, temperatura (0–100 → 0.0–1.0)
- OpenRouter modelo Q&A vs Resumo (`LlmUse.QA` / `SUMMARY`)
- Temperatura aplicada em OpenRouter e on-device (session recreate)

## Resumo

- Livro / mangá: `ReadingSummaryDialog` com seletor + Gerar; mesma persistência de seleção.
- Cache de resumo inclui provider + model id + selectionKey.

## Privacidade / histórico

Inalterados: privacy dinâmica; Room `AssistantHistory`; preload do resumo não persiste no chat.
