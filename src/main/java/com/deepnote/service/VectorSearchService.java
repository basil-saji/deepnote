package com.deepnote.service;

import com.deepnote.ai.OllamaEmbeddingClient;
import com.deepnote.db.Database;
import com.deepnote.model.VectorSearchResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public final class VectorSearchService {
    private final Database database; private final OllamaEmbeddingClient embeddings; private final double threshold;
    public VectorSearchService(Database database,OllamaEmbeddingClient embeddings,double threshold){this.database=database;this.embeddings=embeddings;this.threshold=threshold;}
    public List<VectorSearchResult> search(String query,int limit)throws Exception{
        List<Float> vector=embeddings.embed(query);String literal=NoteIndexService.vectorLiteral(vector);int rawLimit=Math.max(100,limit*10);
        try(Connection c=database.open();PreparedStatement s=c.prepareStatement("SELECT n.file_path,n.title,c.chunk_text,c.chunk_index,1-(c.embedding <=> ?::vector) similarity FROM note_chunks c JOIN notes n ON n.note_id=c.note_id WHERE 1-(c.embedding <=> ?::vector)>=? ORDER BY c.embedding <=> ?::vector LIMIT ?")){s.setString(1,literal);s.setString(2,literal);s.setDouble(3,threshold);s.setString(4,literal);s.setInt(5,rawLimit);try(ResultSet r=s.executeQuery()){List<VectorSearchResult> raw=new ArrayList<>();while(r.next()){Path path=Path.of(r.getString(1));if(Files.isRegularFile(path))raw.add(new VectorSearchResult(path,r.getString(2),r.getString(3),r.getInt(4),r.getDouble(5),r.getDouble(5)));}return rank(raw).stream().limit(limit).toList();}}
    }
    public static List<VectorSearchResult> rank(List<VectorSearchResult> rows){Map<Path,List<VectorSearchResult>> grouped=new LinkedHashMap<>();rows.forEach(r->grouped.computeIfAbsent(r.path(),x->new ArrayList<>()).add(r));List<VectorSearchResult> out=new ArrayList<>();for(var e:grouped.entrySet()){e.getValue().sort(Comparator.comparingDouble(VectorSearchResult::similarity).reversed());var a=e.getValue();double score=a.get(0).similarity()+(a.size()>1?.05*a.get(1).similarity():0)+(a.size()>2?.02*a.get(2).similarity():0);score=Math.min(1.0,score);var best=a.get(0);out.add(new VectorSearchResult(best.path(),best.title(),best.chunkText(),best.chunkIndex(),best.similarity(),score));}out.sort(Comparator.comparingDouble(VectorSearchResult::score).reversed());return out;}
}
