package com.deepnote.service;

import com.deepnote.ai.OllamaEmbeddingClient;
import com.deepnote.db.Database;
import com.deepnote.model.Note;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Rebuilds one note's semantic index atomically; TXT content is always read by the caller. */
public final class NoteIndexService {
    public static final int INDEX_VERSION = 2;
    public record IndexState(boolean exists, boolean hashMatches, boolean versionMatches, boolean hasChunks) {
        public boolean current() { return exists && hashMatches && versionMatches && hasChunks; }
        public boolean emptyNoteCurrent() { return exists && hashMatches && versionMatches && !hasChunks; }
    }
    private final Database database; private final OllamaEmbeddingClient embedder; private final TextChunker chunker;
    public NoteIndexService(Database database, OllamaEmbeddingClient embedder, TextChunker chunker) { this.database=database; this.embedder=embedder; this.chunker=chunker; }
    public IndexState state(Note note) throws SQLException {
        String hash=HashUtil.sha256(note.content());
        try(Connection c=database.open(); PreparedStatement q=c.prepareStatement("SELECT n.indexed_content_hash,n.index_version,COUNT(c.chunk_id),COALESCE(MIN(c.chunk_index),-1),COALESCE(MAX(c.chunk_index),-1) FROM notes n LEFT JOIN note_chunks c ON c.note_id=n.note_id WHERE n.file_path=? GROUP BY n.note_id")) {
            q.setString(1,note.path().toAbsolutePath().toString()); try(ResultSet r=q.executeQuery()){if(!r.next())return new IndexState(false,false,false,false);long count=r.getLong(3);boolean complete=count>0&&r.getInt(4)==0&&r.getInt(5)==count-1;return new IndexState(true,hash.equals(r.getString(1)),r.getInt(2)==INDEX_VERSION,complete);}
        }
    }
    public void index(Note note) throws Exception {
        IndexState existing = state(note);
        if (existing.current() || (note.content().isBlank() && existing.emptyNoteCurrent())) return;
        String hash=HashUtil.sha256(note.content()); List<String> chunks=chunker.split(note.content()); List<List<Float>> vectors=new ArrayList<>(chunks.size());
        for(String chunk:chunks)vectors.add(embedder.embed(chunk));
        if (!Files.readString(note.path(), StandardCharsets.UTF_8).equals(note.content())) throw new IllegalStateException("Note changed while its semantic index was being generated; a newer save will be indexed next.");
        try(Connection c=database.open()){c.setAutoCommit(false);try{UUID id=findOrCreate(c,note);try(PreparedStatement d=c.prepareStatement("DELETE FROM note_chunks WHERE note_id=?")){d.setObject(1,id);d.executeUpdate();}try(PreparedStatement i=c.prepareStatement("INSERT INTO note_chunks(chunk_id,note_id,chunk_index,chunk_text,embedding) VALUES(?,?,?,?,?::vector)")){for(int n=0;n<chunks.size();n++){i.setObject(1,UUID.randomUUID());i.setObject(2,id);i.setInt(3,n);i.setString(4,chunks.get(n));i.setString(5,vectorLiteral(vectors.get(n)));i.addBatch();}i.executeBatch();}try(PreparedStatement u=c.prepareStatement("UPDATE notes SET title=?,file_path=?,indexed_content_hash=?,index_version=?,updated_at=CURRENT_TIMESTAMP,last_indexed_at=CURRENT_TIMESTAMP WHERE note_id=?")){u.setString(1,note.title());u.setString(2,note.path().toAbsolutePath().toString());u.setString(3,hash);u.setInt(4,INDEX_VERSION);u.setObject(5,id);u.executeUpdate();}c.commit();}catch(Exception failure){try{c.rollback();}catch(SQLException ignored){}throw failure;}}
    }
    public void relocate(java.nio.file.Path oldPath, Note renamed) throws SQLException {try(Connection c=database.open();PreparedStatement u=c.prepareStatement("UPDATE notes SET title=?,file_path=?,updated_at=CURRENT_TIMESTAMP WHERE file_path=?")){u.setString(1,renamed.title());u.setString(2,renamed.path().toAbsolutePath().toString());u.setString(3,oldPath.toAbsolutePath().toString());u.executeUpdate();}}
    public void remove(java.nio.file.Path path)throws SQLException{try(Connection c=database.open();PreparedStatement d=c.prepareStatement("DELETE FROM notes WHERE file_path=?")){d.setString(1,path.toAbsolutePath().toString());d.executeUpdate();}}
    public void removeMissing(Set<java.nio.file.Path> existing)throws SQLException{Set<String> normalized=new HashSet<>();for(java.nio.file.Path path:existing)normalized.add(normalize(path));try(Connection c=database.open();PreparedStatement q=c.prepareStatement("SELECT note_id,file_path FROM notes");ResultSet r=q.executeQuery()){List<UUID> stale=new ArrayList<>();while(r.next())if(!normalized.contains(normalize(java.nio.file.Path.of(r.getString(2)))))stale.add((UUID)r.getObject(1));try(PreparedStatement d=c.prepareStatement("DELETE FROM notes WHERE note_id=?")){for(UUID id:stale){d.setObject(1,id);d.addBatch();}d.executeBatch();}}}
    private static String normalize(java.nio.file.Path path){return path.toAbsolutePath().normalize().toString().toLowerCase(Locale.ROOT);}
    private UUID findOrCreate(Connection c,Note note)throws SQLException{String path=note.path().toAbsolutePath().toString();try(PreparedStatement q=c.prepareStatement("SELECT note_id FROM notes WHERE file_path=?")){q.setString(1,path);try(ResultSet r=q.executeQuery()){if(r.next())return(UUID)r.getObject(1);}}UUID id=UUID.randomUUID();try(PreparedStatement i=c.prepareStatement("INSERT INTO notes(note_id,title,file_path,indexed_content_hash,index_version) VALUES(?,?,?,NULL,?)")){i.setObject(1,id);i.setString(2,note.title());i.setString(3,path);i.setInt(4,INDEX_VERSION);i.executeUpdate();}return id;}
    public static String vectorLiteral(List<Float> vector){StringBuilder b=new StringBuilder("[");for(int i=0;i<vector.size();i++){if(i>0)b.append(',');b.append(vector.get(i));}return b.append(']').toString();}
}
