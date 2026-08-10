# On-device LLM model

Place the MediaPipe LiteRT model file here before building:

`gemma3-1b-it-int4.task`

Full path:

`BilingualReader/src/main/assets/llm/gemma3-1b-it-int4.task`

## Source

- Hugging Face: [litert-community/Gemma3-1B-IT](https://huggingface.co/litert-community/Gemma3-1B-IT) (INT4 `.task`)
- Expected size: **~555 MB** (accepted range **500–600 MiB** after extract)
- Runtime: `com.google.mediapipe:tasks-genai:0.10.27` (required for Gemma 3)

The `.task` file is gitignored. Download it locally from the LiteRT community Gemma 3 1B IT INT4 release and drop it into this folder before building a debug/release APK that includes on-device AI.
