# LLM On-Device / Cloud Services

## Componentes

| Classe | Papel |
|--------|--------|
| `LlmBackend` / `LlmBackendFactory` | Escolhe backend efetivo (Auto / On-device / OpenRouter) |
| `OnDeviceLlmBackend` | Extrai `.task` e roda MediaPipe GenAI |
| `OpenRouterLlmBackend` + `OpenRouterClient` | Chat completions SSE; lista modelos free via `GET /api/v1/models` |
| `LlmModelManager` | Extrai o `.task` empacotado (`assets/llm/`) para `filesDir/llm/` |
| `LlmInferenceEngine` | Wrapper MediaPipe `tasks-genai` (`LlmInference` + `LlmInferenceSession`; GPU com fallback CPU) |
| `LlmPromptBuilder` | Prompts system+user; Gemma IT wrap só no on-device |
| `ChapterSummaryService` | Capítulos/páginas selecionados + cache SharedPreferences (inclui model OpenRouter) |
| `BookTextExtractor` | HTML → texto; seleção de ranges de capítulo |
| `BookContextProvider` / `MangaContextProvider` | Contexto do assistente; OCR com `OcrPageCache` |
| `LlmSettings` | Provider, modelos QA/resumo, limites, temperatura, seleção persistida |
| `MlKitTranslator` | Language ID + Translate on-device |
| `OcrFacade` | ML Kit Text Recognition Latin/Japanese com bounding boxes |

## Provider

Pref `KEYS.LLM.PROVIDER`: `auto` | `on_device` | `openrouter` (default `auto`).

- **AUTO:** MediaPipe se ABI `arm64-v8a`; senão OpenRouter.
- **ON_DEVICE:** exige device físico arm64 + modelo em assets.
- **OPENROUTER:** exige API key (Config ou `secrets.properties` → `OPENROUTER_API_KEY`).

Modelo cloud: pref `OPENROUTER_MODEL` (default `openrouter/free`). Na Config (provider Auto/OpenRouter), o dropdown lista modelos free (`pricing.prompt`/`completion` = 0) + o router `openrouter/free`.

## Requisitos on-device

- Dependência: `com.google.mediapipe:tasks-genai:0.10.27` (Gemma-3 1B).
- Modelo asset: `llm/gemma3-1b-it-int4.task` (~555 MB; faixa válida 500–600 MiB).
- Fonte: Hugging Face `litert-community/Gemma3-1B-IT` (INT4 `.task`).
- MediaPipe GenAI embute `libllm_inference_engine_jni.so` para **`arm64-v8a`**.
- Emuladores / 32-bit: use OpenRouter (Auto ou explícito).
- Engine: `maxTokens=1024` (KV do modelo); topK/temperature na `LlmInferenceSession`.

## Preferências (`GeneralConsts.KEYS.LLM`)

- `ENABLED`, `PROVIDER`, `OPENROUTER_API_KEY`, `OPENROUTER_MODEL` (Q&A), `OPENROUTER_MODEL_SUMMARY`
- `MAX_CONTEXT_CHARS`, `MAX_BOOK_CHAPTERS`, `MAX_MANGA_PAGES`, `TEMPERATURE` (0–100)
- `SELECTION_PREFIX`, `SUMMARY_CACHE_PREFIX`, `MODEL_PATH` / `MODEL_VERSION` / `MODEL_EXTRACTED`
- Asset: `ASSET_MODEL_PATH` (`llm/gemma3-1b-it-int4.task`)

## Config

Seção **On-device AI** em `fragment_config_system.xml` / `ConfigFragment`: enable, provider, API key, modelos free OpenRouter (Q&A + resumo), status, limpar cópia local, max context / capítulos / páginas, temperatura.
