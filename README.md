# DeepNote

DeepNote is an offline-first JavaFX desktop note-taking application. Notes are ordinary UTF-8 `.txt` files, local keyword search is independent from AI, and an optional right-side Ollama assistant answers questions about the currently open note.

## Requirements

- Java 25 LTS
- Maven 3.9+
- Ollama running at `http://localhost:11434` for chat
- A local generative Ollama model. The current default is `qwen2.5:0.5b`.

PostgreSQL and pgvector are intentionally not required for this first working version. Semantic/vector search and indexing are deferred until the database phase.

## Build and run

```bash
mvn test
mvn package
mvn compile javafx:run
```

`mvn compile javafx:run` launches the JavaFX workspace. The first run creates the notes directory at `~/DeepNote/notes` (override it with `-Ddeepnote.notesDir=/path/to/notes`). The legacy Swing implementation is retained only for comparison and can be launched explicitly with `mvn exec:java -Dexec.mainClass=com.deepnote.DeepNoteApplication`.

## Ollama chat configuration

The app communicates only with the local Ollama `/api/chat` endpoint. Configure the endpoint, model, timeout, and context budget with JVM properties:

```bash
mvn compile javafx:run -Ddeepnote.chatModel=qwen2.5:0.5b -Ddeepnote.ollamaUrl=http://localhost:11434
```

The installed `embeddinggemma:300m` model is an embedding model and is not used for natural-language chat. If no suitable chat model is installed, install one yourself, for example:

```bash
ollama pull qwen2.5:0.5b
```

The model name is configurable; the application does not silently call an external API.

## Current scope

Current JavaFX workspace:

- Java 25 LTS, JavaFX 27 and AtlantaFX 3.0.0 Maven configuration
- FXML-backed Home, editor and right-side assistant views
- JavaFX dark workspace with centralized blue/charcoal design tokens
- Homepage-first tab architecture with no left sidebar or reserved left-navigation space

Implemented now:

- Create, open, edit, save, rename and delete UTF-8 TXT notes
- Multiple open-note tabs with unsaved-change prompts
- Local title/content keyword search
- Collapsible right-side AI sidebar per note
- In-memory, note-isolated chat history
- Active editor-buffer grounding, bounded recent turns, explicit context-limit errors
- Background Ollama requests so the JavaFX Application Thread remains responsive
- Deterministic unit tests for note CRUD, safe naming, search/context boundaries and conversation isolation

Deferred deliberately:

- PostgreSQL metadata storage
- pgvector and semantic search/indexing
- EmbeddingGemma integration
- Categories, persistent chat history, whole-vault RAG, cloud APIs and agents

## Project layout

Application code is under `src/main/java/com/deepnote`, tests are under `src/test/java`, and repository conventions are documented in `docs/`.
