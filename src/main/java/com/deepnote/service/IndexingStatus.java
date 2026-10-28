package com.deepnote.service;
import java.nio.file.Path;
public record IndexingStatus(Path path, Phase phase, String message, Throwable error) {
    public enum Phase { CONNECTING, SCANNING, INDEXING, READY, FAILED, UNAVAILABLE }
    public boolean failed() { return phase == Phase.FAILED || phase == Phase.UNAVAILABLE; }
}
