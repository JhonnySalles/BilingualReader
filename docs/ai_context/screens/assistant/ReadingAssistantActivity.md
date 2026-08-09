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
3. Chat com streaming; origem do contexto exibida (legendas / OCR / capítulos).

## Entrada

- Menu livro: Resumir capítulos / Perguntar à IA
- Menu mangá: Perguntar à IA
