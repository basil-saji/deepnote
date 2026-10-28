package com.deepnote.service;

import com.deepnote.ai.EmbeddingProvider;
import com.deepnote.config.AppConfig;
import com.deepnote.db.Database;
import com.deepnote.model.Note;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class IndexingResilienceIntegrationTest {
    private static final class FakeEmbedder implements EmbeddingProvider {
        private final String failTitle; FakeEmbedder(String failTitle){this.failTitle=failTitle;}
        public String modelName(){return "fake";} public int dimensions(){return 768;} public String configurationVersion(){return "fake-v1";}
        public List<Float> embedQuery(String query){return vector();}
        public List<Float> embedDocument(String title,String text){if(failTitle!=null&&(failTitle.equals("*")||title.contains(failTitle)))throw new IllegalStateException("intentional embedding failure");return vector();}
        private static List<Float> vector(){return Collections.nCopies(768,0.01f);}
    }
    private static Database database(){AppConfig c=AppConfig.load();Database db=new Database(c);try{db.migrate();return db;}catch(Exception e){Assumptions.assumeTrue(false,"PostgreSQL unavailable: "+e.getMessage());return null;}}

    @Test void failedReplacementKeepsPreviousChunks() throws Exception {
        Database db=database(); Path dir=Files.createTempDirectory("deepnote-rollback-"); NoteService notes=new NoteService(dir); Note note=notes.createNote("stable"); note=notes.saveNote(note,"old persisted content"); NoteIndexService index=new NoteIndexService(db,new FakeEmbedder(null),new TextChunker(2048,256)); index.index(note); Note changed=notes.saveNote(note,"new content that will fail"); NoteIndexService failing=new NoteIndexService(db,new FakeEmbedder("*"),new TextChunker(2048,256));
        try { assertThrows(IllegalStateException.class,()->failing.index(changed)); try(var c=db.open();var q=c.prepareStatement("SELECT chunk_text FROM note_chunks n JOIN notes x ON x.note_id=n.note_id WHERE x.file_path=?")){q.setString(1,changed.path().toAbsolutePath().toString());try(var r=q.executeQuery()){assertTrue(r.next());assertEquals("old persisted content",r.getString(1));}} }
        finally {index.remove(note.path());notes.deleteNote(note);Files.deleteIfExists(dir);}
    }

    @Test void oneReconciliationFailureDoesNotStopLaterNotes() throws Exception {
        Database db=database(); Path dir=Files.createTempDirectory("deepnote-reconcile-"); NoteService notes=new NoteService(dir); Note bad=notes.createNote("bad note");bad=notes.saveNote(bad,"bad content");Note good=notes.createNote("good note");good=notes.saveNote(good,"good content");NoteIndexService index=new NoteIndexService(db,new FakeEmbedder("bad"),new TextChunker(2048,256));IndexingCoordinator coordinator=new IndexingCoordinator(index,s->{});
        try { var result=coordinator.reconcile(List.of(bad,good)).join(); assertEquals(1,result.failed().size()); assertTrue(index.state(good).current()); }
        finally {coordinator.close();index.remove(bad.path());index.remove(good.path());notes.deleteNote(bad);notes.deleteNote(good);Files.deleteIfExists(dir);}
    }
}
