package com.deepnote.service;

import com.deepnote.ai.EmbeddingProvider;
import com.deepnote.db.Database;
import com.deepnote.model.Note;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

/** Rebuilds one note's semantic index atomically; TXT content is always read by the caller. */
public final class NoteIndexService {
    private static final int INDEX_SCHEMA_VERSION = 2;
    public record IndexState(boolean exists, boolean hashMatches, boolean versionMatches, boolean chunksValid) {
        public boolean current() { return exists && hashMatches && versionMatches && chunksValid; }
        public boolean emptyNoteCurrent() { return exists && hashMatches && versionMatches && chunksValid; }
    }
    private final Database database; private final EmbeddingProvider embedder; private final TextChunker chunker; private final int indexVersion;
    public NoteIndexService(Database database, EmbeddingProvider embedder, TextChunker chunker) {
        this.database=database; this.embedder=embedder; this.chunker=chunker;
        this.indexVersion=INDEX_SCHEMA_VERSION + Math.floorMod((embedder.configurationVersion()+"|"+chunker.configuration()).hashCode(), 1_000_000);
    }
    public int indexVersion() { return indexVersion; }
    public IndexState state(Note note) throws SQLException {
        String hash=HashUtil.sha256(note.content()); List<String> expected=chunker.split(note.content());
        try(Connection c=database.open(); PreparedStatement q=c.prepareStatement("SELECT n.indexed_content_hash,n.index_version,c.chunk_index,c.chunk_text,CASE WHEN c.chunk_id IS NULL THEN TRUE ELSE c.embedding IS NOT NULL AND vector_dims(c.embedding)=? END FROM notes n LEFT JOIN note_chunks c ON c.note_id=n.note_id WHERE n.file_path=? ORDER BY c.chunk_index")) {
            q.setInt(1,embedder.dimensions()); q.setString(2,note.path().toAbsolutePath().toString());
            try(ResultSet r=q.executeQuery()) {
                if(!r.next()) return new IndexState(false,false,false,false);
                boolean hashMatches=hash.equals(r.getString(1)); boolean versionMatches=r.getInt(2)==indexVersion; List<String> actual=new ArrayList<>(); int expectedIndex=0; boolean contiguous=true;
                boolean embeddingValid=true;
                do { int index=r.getInt(3); if(r.wasNull()){ if(!expected.isEmpty())contiguous=false; break; } if(index!=expectedIndex++)contiguous=false; actual.add(r.getString(4)); embeddingValid=embeddingValid&&r.getBoolean(5); } while(r.next());
                boolean chunksValid=contiguous&&embeddingValid&&actual.equals(expected);
                return new IndexState(true,hashMatches,versionMatches,chunksValid);
            }
        }
    }
    public void index(Note note) throws Exception {
        IndexState existing=state(note); if(existing.current()) return;
        String hash=HashUtil.sha256(note.content()); List<String> chunks=chunker.split(note.content()); List<List<Float>> vectors=new ArrayList<>(chunks.size());
        for(String chunk:chunks)vectors.add(embedder.embedDocument(note.title(),chunk));
        if(!Files.readString(note.path(),StandardCharsets.UTF_8).equals(note.content()))throw new IllegalStateException("Note changed while its semantic index was being generated; a newer save will be indexed next.");
        try(Connection c=database.open()){c.setAutoCommit(false);try{UUID id=findOrCreate(c,note);try(PreparedStatement d=c.prepareStatement("DELETE FROM note_chunks WHERE note_id=?")){d.setObject(1,id);d.executeUpdate();}try(PreparedStatement i=c.prepareStatement("INSERT INTO note_chunks(chunk_id,note_id,chunk_index,chunk_text,embedding) VALUES(?,?,?,?,?::vector)")){for(int n=0;n<chunks.size();n++){i.setObject(1,UUID.randomUUID());i.setObject(2,id);i.setInt(3,n);i.setString(4,chunks.get(n));i.setString(5,vectorLiteral(vectors.get(n)));i.addBatch();}i.executeBatch();}try(PreparedStatement u=c.prepareStatement("UPDATE notes SET title=?,file_path=?,indexed_content_hash=?,index_version=?,updated_at=CURRENT_TIMESTAMP,last_indexed_at=CURRENT_TIMESTAMP WHERE note_id=?")){u.setString(1,note.title());u.setString(2,note.path().toAbsolutePath().toString());u.setString(3,hash);u.setInt(4,indexVersion);u.setObject(5,id);u.executeUpdate();}c.commit();}catch(Exception failure){try{c.rollback();}catch(SQLException ignored){}throw failure;}}
    }
    public void relocate(Path oldPath,Note renamed)throws SQLException{try(Connection c=database.open();PreparedStatement u=c.prepareStatement("UPDATE notes SET title=?,file_path=?,updated_at=CURRENT_TIMESTAMP WHERE file_path=?")){u.setString(1,renamed.title());u.setString(2,renamed.path().toAbsolutePath().toString());u.setString(3,oldPath.toAbsolutePath().toString());u.executeUpdate();}}
    public void remove(Path path)throws SQLException{try(Connection c=database.open();PreparedStatement d=c.prepareStatement("DELETE FROM notes WHERE file_path=?")){d.setString(1,path.toAbsolutePath().toString());d.executeUpdate();}}
    public void removeMissing(Set<Path> existing)throws SQLException{Set<String> normalized=new HashSet<>();for(Path path:existing)normalized.add(normalize(path));try(Connection c=database.open();PreparedStatement q=c.prepareStatement("SELECT note_id,file_path FROM notes");ResultSet r=q.executeQuery()){List<UUID> stale=new ArrayList<>();while(r.next())if(!normalized.contains(normalize(Path.of(r.getString(2)))))stale.add((UUID)r.getObject(1));try(PreparedStatement d=c.prepareStatement("DELETE FROM notes WHERE note_id=?")){for(UUID id:stale){d.setObject(1,id);d.addBatch();}d.executeBatch();}}}
    private static String normalize(Path path){return path.toAbsolutePath().normalize().toString().toLowerCase(Locale.ROOT);}
    private UUID findOrCreate(Connection c,Note note)throws SQLException{String path=note.path().toAbsolutePath().toString();try(PreparedStatement q=c.prepareStatement("SELECT note_id FROM notes WHERE file_path=?")){q.setString(1,path);try(ResultSet r=q.executeQuery()){if(r.next())return(UUID)r.getObject(1);}}UUID id=UUID.randomUUID();try(PreparedStatement i=c.prepareStatement("INSERT INTO notes(note_id,title,file_path,indexed_content_hash,index_version) VALUES(?,?,?,NULL,?)")){i.setObject(1,id);i.setString(2,note.title());i.setString(3,path);i.setInt(4,indexVersion);i.executeUpdate();}return id;}
    public static String vectorLiteral(List<Float> vector){StringBuilder b=new StringBuilder("[");for(int i=0;i<vector.size();i++){if(i>0)b.append(',');b.append(vector.get(i));}return b.append(']').toString();}
}
