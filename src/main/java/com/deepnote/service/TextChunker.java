package com.deepnote.service;

import java.util.*;

/** Paragraph-aware character approximation of the SDD's 512-token/64-token target. */
public final class TextChunker {
    private final int maxCharacters, overlapCharacters;
    public TextChunker(int maxCharacters) { this(maxCharacters, 0); }
    public TextChunker(int maxCharacters, int overlapCharacters) {
        if (maxCharacters < 1 || overlapCharacters < 0 || overlapCharacters >= maxCharacters) throw new IllegalArgumentException("Invalid chunk limits");
        this.maxCharacters=maxCharacters; this.overlapCharacters=overlapCharacters;
    }
    public String configuration() { return maxCharacters + ":" + overlapCharacters; }
    public List<String> split(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> result=new ArrayList<>();
        for(String paragraph:text.split("\\R\\s*\\R")) {
            String remaining=paragraph.trim();
            while(!remaining.isEmpty()) {
                int end=Math.min(maxCharacters,remaining.length());
                if(end<remaining.length()){int space=remaining.lastIndexOf(' ',end);if(space>maxCharacters/2)end=space;}
                String chunk=remaining.substring(0,end).trim();if(!chunk.isEmpty())result.add(chunk);
                if(end>=remaining.length())break;
                int start=Math.max(0,end-overlapCharacters);while(start<remaining.length()&&Character.isWhitespace(remaining.charAt(start)))start++;
                remaining=remaining.substring(start).trim();
            }
        }
        return result;
    }
}
