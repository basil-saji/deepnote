package com.deepnote.service;

import com.deepnote.model.Note;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Serializes indexing per note and coalesces saves arriving during an active pass. */
public final class IndexingCoordinator implements AutoCloseable {
    public record ReconciliationResult(int scanned, int indexed, List<Path> failed) { public boolean complete() { return failed.isEmpty(); } }
    private static final class Work { Note latest; boolean active; boolean pending; }
    private final NoteIndexService indexService; private final ExecutorService executor; private final Map<Path,Work> work=new HashMap<>(); private final Consumer<IndexingStatus> status;
    public IndexingCoordinator(NoteIndexService service, Consumer<IndexingStatus> status){indexService=service;this.status=status;executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"deepnote-indexer");t.setDaemon(true);return t;});}
    public void request(Note note){Path key=note.path().toAbsolutePath();synchronized(work){Work w=work.computeIfAbsent(key,k->new Work());w.latest=note;if(w.active){w.pending=true;return;}w.active=true;executor.execute(()->run(key,w));}}
    public CompletableFuture<ReconciliationResult> reconcile(List<Note> notes){return CompletableFuture.supplyAsync(()->{report(null,IndexingStatus.Phase.SCANNING,"Scanning notes",null);try{indexService.removeMissing(notes.stream().map(n->n.path().toAbsolutePath()).collect(java.util.stream.Collectors.toSet()));}catch(Exception e){report(null,IndexingStatus.Phase.FAILED,"Could not remove stale index metadata",e);throw new CompletionException(e);}List<Path> failed=new ArrayList<>();int indexed=0;for(int i=0;i<notes.size();i++){Note note=notes.get(i);report(note.path(),IndexingStatus.Phase.SCANNING,"Scanning note "+(i+1)+" of "+notes.size(),null);try{NoteIndexService.IndexState state=indexService.state(note);if(!state.current()){report(note.path(),IndexingStatus.Phase.INDEXING,"Indexing "+(i+1)+" of "+notes.size(),null);indexService.index(note);indexed++;}}catch(Exception e){failed.add(note.path());report(note.path(),IndexingStatus.Phase.FAILED,"Indexing failed",e);}}ReconciliationResult result=new ReconciliationResult(notes.size(),indexed,List.copyOf(failed));report(null,result.complete()?IndexingStatus.Phase.READY:IndexingStatus.Phase.FAILED,result.complete()?"Ready":"Ready with "+failed.size()+" indexing failure(s)",null);return result;},executor);}
    public void delete(Path path){executor.execute(()->{try{indexService.remove(path);}catch(Exception e){report(path,IndexingStatus.Phase.FAILED,"Index cleanup failed",e);}});}
    public void relocate(Path oldPath, Note renamed){executor.execute(()->{try{indexService.relocate(oldPath,renamed);}catch(Exception e){report(renamed.path(),IndexingStatus.Phase.FAILED,"Index rename cleanup failed",e);}});}
    private void run(Path key,Work w){while(true){Note note;synchronized(work){note=w.latest;w.pending=false;}try{report(key,IndexingStatus.Phase.INDEXING,"Indexing "+note.title(),null);indexService.index(note);report(key,IndexingStatus.Phase.READY,"Index current",null);}catch(Exception e){report(key,IndexingStatus.Phase.FAILED,"Indexing failed",e);}synchronized(work){if(w.pending)continue;w.active=false;work.remove(key);return;}}}
    private void report(Path path,IndexingStatus.Phase phase,String message,Throwable error){status.accept(new IndexingStatus(path,phase,message,error));}
    @Override public void close(){executor.shutdownNow();}
}
