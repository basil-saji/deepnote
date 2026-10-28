package com.deepnote.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TextChunkerTest {
 @Test void createsOrderedNonEmptyChunks(){var chunks=new TextChunker(8).split("one two three four");assertEquals(java.util.List.of("one two","three","four"),chunks);assertTrue(chunks.stream().noneMatch(String::isBlank));}
 @Test void hashIsStableForUnchangedContent(){assertEquals(HashUtil.sha256("same"),HashUtil.sha256("same"));assertNotEquals(HashUtil.sha256("same"),HashUtil.sha256("changed"));}
 @Test void productionOverlapPreservesContextWithoutEmptyChunks(){var chunks=new TextChunker(20,5).split("one two three four five six seven");assertTrue(chunks.size()>1);assertTrue(chunks.stream().noneMatch(String::isBlank));}
}
