package com.deepnote.db;

import com.deepnote.config.AppConfig;
import java.sql.*;

/** Small JDBC boundary. Passwords are deliberately read only from the environment. */
public final class Database {
    private final AppConfig config;
    public Database(AppConfig config) { this.config = config; }
    public Connection open() throws SQLException {
        if (config.dbPassword().isBlank()) throw new SQLException("DB_PASSWORD is not configured.");
        return DriverManager.getConnection("jdbc:postgresql://" + config.dbHost() + ":" + config.dbPort() + "/" + config.dbName(), config.dbUser(), config.dbPassword());
    }
    public void migrate() throws SQLException {
        try (Connection c = open(); Statement s = c.createStatement()) {
            s.execute("CREATE EXTENSION IF NOT EXISTS vector");
            s.execute("CREATE TABLE IF NOT EXISTS categories (category_id UUID PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE)");
            s.execute("CREATE TABLE IF NOT EXISTS notes (note_id UUID PRIMARY KEY, title VARCHAR(255) NOT NULL, file_path TEXT NOT NULL UNIQUE, indexed_content_hash CHAR(64), category_id UUID REFERENCES categories(category_id), index_version INT NOT NULL DEFAULT 1, last_indexed_at TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            s.execute("ALTER TABLE notes ADD COLUMN IF NOT EXISTS index_version INT NOT NULL DEFAULT 1");
            s.execute("CREATE TABLE IF NOT EXISTS note_chunks (chunk_id UUID PRIMARY KEY, note_id UUID NOT NULL REFERENCES notes(note_id) ON DELETE CASCADE, chunk_index INT NOT NULL, chunk_text TEXT NOT NULL, embedding VECTOR(768) NOT NULL, CONSTRAINT uq_chunk_order UNIQUE(note_id, chunk_index))");
            s.execute("CREATE INDEX IF NOT EXISTS idx_notes_category ON notes(category_id)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_chunks_note ON note_chunks(note_id)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_chunks_order ON note_chunks(note_id, chunk_index)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_chunks_embedding ON note_chunks USING hnsw (embedding vector_cosine_ops)");
        }
    }
}
