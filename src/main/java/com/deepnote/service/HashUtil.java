package com.deepnote.service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
public final class HashUtil {
    private HashUtil() {}
    public static String sha256(String value) { try { byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder b=new StringBuilder(); for(byte x:bytes)b.append(String.format("%02x",x)); return b.toString(); } catch(Exception e){ throw new IllegalStateException(e); } }
}
