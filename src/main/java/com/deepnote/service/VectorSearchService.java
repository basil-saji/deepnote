package com.deepnote.service;

import com.deepnote.ai.EmbeddingProvider;
import com.deepnote.db.Database;
import com.deepnote.model.Note;
import com.deepnote.model.VectorSearchResult;
import com.deepnote.model.VectorSearchResult.MatchKind;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

/** Combines thresholded semantic retrieval with an explicit TXT-backed lexical rescue path. */
public final class VectorSearchService {
    private final Database database; private final EmbeddingProvider embeddings; private final double threshold;
    private final LocalSearchService lexicalSearch = new LocalSearchService(); private final Path notesDirectory;
    public VectorSearchService(Database database,EmbeddingProvider embeddings,double threshold){this(database,embeddings,threshold,null);}
    public VectorSearchService(Database database,EmbeddingProvider embeddings,double threshold,Path notesDirectory){this.database=database;this.embeddings=embeddings;this.threshold=threshold;this.notesDirectory=notesDirectory;}

    public List<VectorSearchResult> search(String query,int limit)throws Exception{
        if (query == null || query.isBlank()) return List.of();
        List<Float> vector=embeddings.embedQuery(query); String literal=NoteIndexService.vectorLiteral(vector); int rawLimit=Math.max(100,limit*10);
        Map<Path,VectorSearchResult> semantic=new LinkedHashMap<>();
        String sql="WITH candidates AS (SELECT n.file_path,n.title,c.chunk_text,c.chunk_index,1-(c.embedding <=> ?::vector) similarity,ROW_NUMBER() OVER (PARTITION BY c.note_id ORDER BY c.embedding <=> ?::vector) AS chunk_rank FROM note_chunks c JOIN notes n ON n.note_id=c.note_id WHERE 1-(c.embedding <=> ?::vector)>=?) SELECT file_path,title,chunk_text,chunk_index,similarity FROM candidates WHERE chunk_rank<=3 ORDER BY similarity DESC LIMIT ?";
        try(Connection c=database.open();PreparedStatement s=c.prepareStatement(sql)){s.setString(1,literal);s.setString(2,literal);s.setString(3,literal);s.setDouble(4,threshold);s.setInt(5,rawLimit);try(ResultSet r=s.executeQuery()){List<VectorSearchResult> raw=new ArrayList<>();while(r.next()){Path path=Path.of(r.getString(1));if(Files.isRegularFile(path))raw.add(new VectorSearchResult(path,r.getString(2),r.getString(3),r.getInt(4),r.getDouble(5),r.getDouble(5),MatchKind.SEMANTIC));}for(VectorSearchResult result:rank(raw))semantic.put(result.path(),result);}}
        return mergeResults(new ArrayList<>(semantic.values()), lexicalMatches(query), query, limit);
    }

    private List<VectorSearchResult> lexicalMatches(String query)throws SQLException{
        List<VectorSearchResult> matches=new ArrayList<>();
        List<Note> notes=new ArrayList<>();
        if(notesDirectory!=null&&Files.isDirectory(notesDirectory)){try(var paths=Files.list(notesDirectory)){paths.filter(p->p.toString().toLowerCase(Locale.ROOT).endsWith(".txt")).forEach(path->{try{notes.add(new Note(title(path),path,Files.readString(path,StandardCharsets.UTF_8)));}catch(Exception ignored){}});}catch(java.io.IOException ignored){}}
        else try(Connection c=database.open();PreparedStatement q=c.prepareStatement("SELECT file_path,title FROM notes");ResultSet r=q.executeQuery()){while(r.next()){Path path=Path.of(r.getString(1));if(Files.isRegularFile(path))try{notes.add(new Note(r.getString(2),path,Files.readString(path,StandardCharsets.UTF_8)));}catch(Exception ignored){}}}
        for(Note note:lexicalSearch.search(notes,query))matches.add(new VectorSearchResult(note.path(),note.title(),snippet(note.content(),query),-1,Double.NaN,Double.NaN,MatchKind.LEXICAL));
        return matches;
    }

    private static String title(Path path){String name=path.getFileName().toString();return name.toLowerCase(Locale.ROOT).endsWith(".txt")?name.substring(0,name.length()-4):name;}

    private static String snippet(String content,String query){String flat=content.replaceAll("\\s+"," ").trim();int hit=flat.toLowerCase(Locale.ROOT).indexOf(query.trim().toLowerCase(Locale.ROOT));if(hit<0)return flat.length()>220?flat.substring(0,220)+"…":flat;int start=Math.max(0,hit-80),end=Math.min(flat.length(),hit+query.trim().length()+140);return (start>0?"…":"")+flat.substring(start,end)+(end<flat.length()?"…":"");}
    private static Comparator<VectorSearchResult> resultComparator(){return Comparator.comparingInt((VectorSearchResult r)->switch(r.matchKind()){case BOTH->0;case LEXICAL->1;case SEMANTIC->2;}).thenComparing((a,b)->{if(a.hasSemanticScore()&&b.hasSemanticScore())return Double.compare(b.score(),a.score());return 0;}).thenComparing(r->r.title().toLowerCase(Locale.ROOT));}
    public static List<VectorSearchResult> mergeResults(List<VectorSearchResult> semantic,List<VectorSearchResult> lexical,String query,int limit){Map<Path,VectorSearchResult> merged=new LinkedHashMap<>();semantic.forEach(r->merged.put(r.path(),r));String needle=query.trim().toLowerCase(Locale.ROOT);for(VectorSearchResult match:lexical){VectorSearchResult prior=merged.get(match.path());if(prior==null)merged.put(match.path(),match);else{boolean hasPassage=match.chunkIndex()<0&&match.chunkText().toLowerCase(Locale.ROOT).contains(needle);merged.put(match.path(),new VectorSearchResult(prior.path(),prior.title(),hasPassage?match.chunkText():prior.chunkText(),hasPassage?match.chunkIndex():prior.chunkIndex(),prior.similarity(),prior.score(),MatchKind.BOTH));}}List<VectorSearchResult> result=new ArrayList<>(merged.values());result.sort(resultComparator());return result.stream().limit(limit).toList();}
    public static List<VectorSearchResult> rank(List<VectorSearchResult> rows){Map<Path,List<VectorSearchResult>> grouped=new LinkedHashMap<>();rows.forEach(r->grouped.computeIfAbsent(r.path(),x->new ArrayList<>()).add(r));List<VectorSearchResult> out=new ArrayList<>();for(var e:grouped.entrySet()){e.getValue().sort(Comparator.comparingDouble(VectorSearchResult::similarity).reversed());var a=e.getValue();double score=a.get(0).similarity()+(a.size()>1?.05*a.get(1).similarity():0)+(a.size()>2?.02*a.get(2).similarity():0);score=Math.min(1.0,score);var best=a.get(0);out.add(new VectorSearchResult(best.path(),best.title(),best.chunkText(),best.chunkIndex(),best.similarity(),score,MatchKind.SEMANTIC));}out.sort(Comparator.comparingDouble(VectorSearchResult::score).reversed());return out;}
}
