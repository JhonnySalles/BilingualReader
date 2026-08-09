# LLM On-Device Services

## Componentes

| Classe | Papel |
|--------|--------|
| `LlmModelManager` | Download sob demanda do `.task` (Gemma 3 1B) para `filesDir/llm/` |
| `LlmInferenceEngine` | Wrapper MediaPipe `tasks-genai`; mock em debug (`USE_MOCK_LLM`) |
| `LlmPromptBuilder` | Prompts de resumo e Q&A + truncamento |
| `ChapterSummaryService` | Últimos 3 capítulos + cache SharedPreferences |
| `BookTextExtractor` | HTML → texto; seleção de ranges de capítulo |
| `MlKitTranslator` | Language ID + Translate on-device |
| `OcrFacade` | ML Kit Text Recognition Latin/Japanese com bounding boxes |

## Preferências (`GeneralConsts.KEYS.LLM`)

- `ENABLED`, `MODEL_URL`, `MODEL_PATH`, `MODEL_VERSION`, `MAX_CONTEXT_CHARS`, cache de resumo

## Config

Seção **On-device AI** em `fragment_config_system.xml` / `ConfigFragment`.
