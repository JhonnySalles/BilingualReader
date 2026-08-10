# ReadingAssistantActivity

## Objetivo

Tela de perguntas over on-device LLM (MediaPipe Gemma 3 1B) para livro e mangá. O usuário pergunta sobre a obra em leitura; o contexto vem dos últimos capítulos (livro) ou legendas/OCR híbrido (mangá).

## Arquivos

- [ReadingAssistantActivity.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/assistant/ReadingAssistantActivity.kt)
- [ReadingAssistantViewModel.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/view/ui/assistant/ReadingAssistantViewModel.kt)
- [activity_reading_assistant.xml](BilingualReader/src/main/res/layout/activity_reading_assistant.xml)
- [AssistantSessionHolder.kt](BilingualReader/src/main/kotlin/br/com/fenix/bilingualreader/service/llm/AssistantSessionHolder.kt)
- Providers: `BookContextProvider`, `MangaContextProvider`
- Gate de modelo: `LlmModelGate`

## Fluxo

1. Leitor prepara sessão via `ReadingAssistantActivity.prepareBook/prepareManga`.
2. Activity abre, garante download/load do modelo, monta contexto.
3. Carrega histórico local (`AssistantHistory` por `id_reference` + `type`) e exibe no chat.
4. Chat com streaming; ao finalizar cada turno USER/ASSISTANT, grava no Room (exceto preload do dialog e mensagens SYSTEM).
5. Menu da toolbar permite limpar o histórico da obra atual.

## Entrada

- Menu livro: Resumir capítulos / Perguntar à IA
- Menu mangá: Perguntar à IA

## Persistência

- Tabela Room `AssistantHistory` (v4): `id_reference`, `type` (BOOK/MANGA), `role` (USER/ASSISTANT), `message`, `date`.
- `ChapterSummaryDialog` não grava neste histórico; `preloadSummary` só aparece na UI.
