# LLM On-Device Services

## Componentes

| Classe | Papel |
|--------|--------|
| `LlmModelManager` | Extrai o `.task` empacotado (`assets/llm/`) para `filesDir/llm/` na primeira carga |
| `LlmInferenceEngine` | Wrapper MediaPipe `tasks-genai` (GPU com fallback CPU) |
| `LlmPromptBuilder` | Prompts de resumo e Q&A (template Gemma IT) + truncamento |
| `ChapterSummaryService` | Últimos 3 capítulos + cache SharedPreferences |
| `BookTextExtractor` | HTML → texto; seleção de ranges de capítulo |
| `MlKitTranslator` | Language ID + Translate on-device |
| `OcrFacade` | ML Kit Text Recognition Latin/Japanese com bounding boxes |

## Requisitos de dispositivo

- MediaPipe GenAI embute `libllm_inference_engine_jni.so` para **`arm64-v8a`**.
- **Requer aparelho físico 64-bit.** Emuladores e ABIs 32-bit (`armeabi-v7a`) / x86 não são suportados; o app mostra `llm_error_unsupported_device` em vez de crashar.
- `LlmInferenceEngine.isNativeBackendAvailable()` checa ABI **antes** de carregar a classe `LlmInference` (cujo `<clinit>` chama `System.loadLibrary`).

## Preferências (`GeneralConsts.KEYS.LLM`)

- `ENABLED`, `MODEL_PATH`, `MODEL_VERSION`, `MODEL_EXTRACTED`, `MAX_CONTEXT_CHARS`, cache de resumo
- Asset: `ASSET_MODEL_PATH` (`llm/gemma3-1b-it-int4.task`)

## Config

Seção **On-device AI** em `fragment_config_system.xml` / `ConfigFragment`.
