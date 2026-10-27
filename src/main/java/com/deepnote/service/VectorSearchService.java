package com.deepnote.service;

import com.deepnote.ai.OllamaEmbeddingClient; import com.deepnote.db.Database; import com.deepnote.model.VectorSearchResult;
import java.sql.*; import java.util.*;

public final class VectorSearchService {
    private final Database database; private final OllamaEmbeddingClient embeddings; private final double threshold;
    public VectorSearchService(Database database, OllamaEmbeddingClient embeddings, double threshold){this.database=database;this.embeddings=embeddings;this.threshold=threshold;}
    public List<VectorSearchResult> search(String query, int limit) throws Exception {
        List<Float> vector=embeddings.embed(query); String v=vector.toString();
        try(Connection c=database.open(); PreparedStatement s=c.prepareStatement("SELECT n.file_path,n.title,c.chunk_text,c.chunk_index,1-(c.embedding <=> ?::vector) similarity FROM note_chunks c JOIN notes n ON n.note_id=c.note_id WHERE 1-(c.embedding <=> ?::vector)>=? ORDER BY c.embedding <=> ?::vector LIMIT ?")){s.setString(1,v);s.setString(2,v);s.setDouble(3,threshold);s.setString(4,v);s.setInt(5,limit);try(ResultSet r=s.executeQuery()){List<VectorSearchResult> raw=new ArrayList<>();while(r.next())raw.add(new VectorSearchResult(java.nio.file.Path.of(r.getString(1)),r.getString(2),r.getString(3),r.getInt(4),r.getDouble(5),r.getDouble(5)));return rank(raw);}}
    }
    static List<VectorSearchResult> rank(List<VectorSearchResult> rows){Map<java.nio.file.Path,List<VectorSearchResult>> grouped=new LinkedHashMap<>(); rows.forEach(r->grouped.computeIfAbsent(r.path(),x->new ArrayList<>()).add(r)); List<VectorSearchResult> out=new ArrayList<>(); for(var e:grouped.entrySet()){e.getValue().sort(Comparator.comparingDouble(VectorSearchResult::similarity).reversed());var a=e.getValue();double score=a.get(0).similarity()+(a.size()>1?.05*a.get(1).similarity():0)+(a.size()>2?.02*a.get(2).similarity():0);var best=a.get(0);out.add(new VectorSearchResult(best.path(),best.title(),best.chunkText(),best.chunkIndex(),best.similarity(),score));}out.sort(Comparator.comparingDouble(VectorSearchResult::score).reversed());return out;}
}
