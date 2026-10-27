# DeepNote

DeepNote is an offline-first JavaFX desktop note-taking application. Notes are ordinary UTF-8 `.txt` files, local keyword search is independent from AI, and an optional right-side Ollama assistant answers questions about the currently open note.

## Requirements

- Java 25 LTS
- Maven 3.9+
- Ollama v0.11.10 or newer running at `http://localhost:11434` for chat and embeddings
- A local generative Ollama model. The current default is `qwen2.5:0.5b`.

- PostgreSQL with pgvector is used for optional semantic indexing/search. TXT files remain the source of truth and keyword search works without either service.

## Build and run

```bash
mvn test
mvn package
mvn compile javafx:run
```

`mvn compile javafx:run` launches the JavaFX workspace. The first run creates `%USERPROFILE%\\Documents\\DeepNote\\notes` on Windows (override it with `-Ddeepnote.notesDir=/path/to/notes`). Existing legacy `~/DeepNote/notes` files are not moved or overwritten; copy them manually only after checking for name collisions.

Start the local database with `docker compose up -d`. The compose file uses `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, and `POSTGRES_PORT`; the application uses matching `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and required `DB_PASSWORD` variables. On startup, DeepNote applies the repeatable schema migration in `db/migrations/V1__initial_schema.sql`/the JDBC migration path and never drops existing data.

For desktop launches from the repository root, the application also reads the ignored local `.env` file using the same names; process environment variables take precedence. Keep `.env` local and never commit it.

Install the local embedding model with `ollama pull embeddinggemma:300m`. Older Ollama releases may report that this model does not support embeddings; upgrade Ollama first, then pull the model again. DeepNote uses EmbeddingGemma's retrieval prompts (`task: search result | query: ...` and `title: ... | text: ...`) and verifies exactly 768 dimensions. The default similarity threshold is disabled (`-1.0`): retrieval takes nearest chunks first; set `-Ddeepnote.similarityThreshold=...` only after measuring the local corpus.

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

## Search modes

The Home search toggle provides `Vector` and `Keyword`. Keyword mode searches TXT files locally. Vector mode embeds queries and note chunks through local Ollama, searches pgvector with cosine distance, groups chunks by note, and shows a relevance score. Database/Ollama failures are reported as vector-search errors and do not disable keyword search.

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

Implemented database phase:

- PostgreSQL metadata storage, pgvector indexing and semantic search
- SHA-256 change detection, deterministic paragraph chunking and transactional replacement
- Local EmbeddingGemma indexing (when PostgreSQL and Ollama are available)
- Categories, persistent chat history, whole-vault RAG, cloud APIs and agents

## Project layout

Application code is under `src/main/java/com/deepnote`, tests are under `src/test/java`, and repository conventions are documented in `docs/`.
