# DeepNote

DeepNote is an offline-first desktop note-taking application. The existing Swing implementation remains intact while Phase 1 establishes the JavaFX 27 + AtlantaFX 3.0.0 migration foundation. Notes are ordinary UTF-8 `.txt` files, local keyword search is independent from AI, and an optional right-side Ollama assistant answers questions about the currently open note.

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
mvn javafx:run
```

`mvn javafx:run` opens the Phase 1 JavaFX/AtlantaFX foundation smoke window. The existing Swing application remains available through its `com.deepnote.DeepNoteApplication` launcher while the full JavaFX wireframe migration is developed separately. The first run creates the notes directory at `~/DeepNote/notes` (override it with `-Ddeepnote.notesDir=/path/to/notes`).

## Ollama chat configuration

The app communicates only with the local Ollama `/api/chat` endpoint. Configure the endpoint, model, timeout, and context budget with JVM properties:

```bash
mvn exec:java -Dexec.jvmArgs="-Ddeepnote.chatModel=qwen2.5:0.5b -Ddeepnote.ollamaUrl=http://localhost:11434"
```

The installed `embeddinggemma:300m` model is an embedding model and is not used for natural-language chat. If no suitable chat model is installed, install one yourself, for example:

```bash
ollama pull qwen2.5:0.5b
```

The model name is configurable; the application does not silently call an external API.

## Current scope

Phase 1 foundation:

- Java 25 LTS, JavaFX 27 and AtlantaFX 3.0.0 Maven configuration
- FXML loading, JavaFX controller seam and centralized foundation CSS
- Independently launchable JavaFX dark-theme smoke window
- Homepage-first target architecture with no left sidebar or reserved left-navigation space

The complete JavaFX homepage/editor wireframe is deliberately deferred to the next migration phase.

Implemented now:

- Create, open, edit, save, rename and delete UTF-8 TXT notes
- Multiple open-note tabs with unsaved-change prompts
- Local title/content keyword search
- Collapsible right-side AI sidebar per note
- In-memory, note-isolated chat history
- Active editor-buffer grounding, bounded recent turns, explicit context-limit errors
- Background Ollama requests so the Swing EDT remains responsive
- Deterministic unit tests for note CRUD, safe naming, search/context boundaries and conversation isolation

Deferred deliberately:

- PostgreSQL metadata storage
- pgvector and semantic search/indexing
- EmbeddingGemma integration
- Categories, persistent chat history, whole-vault RAG, cloud APIs and agents

## Project layout

Application code is under `src/main/java/com/deepnote`, tests are under `src/test/java`, and repository conventions are documented in `docs/`.
