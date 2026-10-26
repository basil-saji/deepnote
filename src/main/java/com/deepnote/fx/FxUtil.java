package com.deepnote.fx;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/** Small shared helpers for the JavaFX layer: async work, icons, time/metric formatting, match highlighting. */
final class FxUtil {
    // 16x16 stroke icons, styled by the .icon-path CSS class.
    static final String SEARCH = "M7 12.5a5.5 5.5 0 1 1 0-11a5.5 5.5 0 0 1 0 11z M11 11l3.5 3.5";
    static final String DOC = "M4 1.5h5.5L13 5v9.5H4z M9.5 1.5V5H13";
    static final String PENCIL = "M11.5 2.5l2 2L5 13l-3 1l1-3z M10 4l2 2";
    static final String TRASH = "M2.5 4.5h11 M6 4.5V3h4v1.5 M4 4.5l.7 8.5h6.6l.7-8.5 M6.8 7v4 M9.2 7v4";
    static final String SAVE = "M8 2v7.5 M5 7l3 3l3-3 M2.5 10v3.5h11V10";

    private FxUtil() { }

    /** Runs blocking work off the FX thread, then delivers the result (or failure) on the FX thread. */
    static <T> void async(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        CompletableFuture.supplyAsync(() -> {
            try { return work.call(); } catch (Exception ex) { throw new CompletionException(ex); }
        }).whenComplete((value, error) -> Platform.runLater(() -> {
            if (error != null) {
                Throwable cause = error;
                while (cause instanceof CompletionException && cause.getCause() != null) cause = cause.getCause();
                onFailure.accept(cause);
            } else onSuccess.accept(value);
        }));
    }

    static Node icon(String pathData) {
        SVGPath path = new SVGPath();
        path.setContent(pathData);
        path.getStyleClass().add("icon-path");
        return path;
    }

    static void show(Node node, boolean visible) { node.setVisible(visible); node.setManaged(visible); }

    static void toggleClass(Node node, String styleClass, boolean on) {
        node.getStyleClass().remove(styleClass);
        if (on) node.getStyleClass().add(styleClass);
    }

    static long wordCount(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
    }

    static int readMinutes(long words) { return Math.max(1, Math.round(words / 200f)); }

    static String relativeTime(Instant time) {
        if (time == null || time.equals(Instant.EPOCH)) return "unknown";
        Duration age = Duration.between(time, Instant.now());
        long minutes = age.toMinutes();
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + "m ago";
        if (age.toHours() < 24) return age.toHours() + "h ago";
        if (age.toDays() < 7) return age.toDays() + "d ago";
        ZonedDateTime when = time.atZone(ZoneId.systemDefault());
        boolean sameYear = when.getYear() == ZonedDateTime.now().getYear();
        return when.format(DateTimeFormatter.ofPattern(sameYear ? "MMM d" : "MMM d, yyyy", Locale.ENGLISH));
    }

    static int indexOfIgnoreCase(String haystack, String needle, int from) {
        int last = haystack.length() - needle.length();
        for (int i = Math.max(0, from); i <= last; i++) if (haystack.regionMatches(true, i, needle, 0, needle.length())) return i;
        return -1;
    }

    /** ~200 character excerpt of the note body, centred on the first match of the query. */
    static String snippet(String content, String query) {
        String flat = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (flat.isEmpty()) return "Empty note";
        String q = query == null ? "" : query.trim();
        int hit = q.isEmpty() ? -1 : indexOfIgnoreCase(flat, q, 0);
        int start = hit < 0 ? 0 : Math.max(0, hit - 70);
        int end = Math.min(flat.length(), start + 200);
        return (start > 0 ? "…" : "") + flat.substring(start, end) + (end < flat.length() ? "…" : "");
    }

    /** Builds a TextFlow where every case-insensitive occurrence of {@code query} is shown as a highlighted chip. */
    static TextFlow highlight(String text, String query, String textClass) {
        TextFlow flow = new TextFlow();
        flow.setMinWidth(0);
        flow.setMaxWidth(Double.MAX_VALUE);
        String q = query == null ? "" : query.trim();
        int from = 0;
        if (!q.isEmpty()) {
            int hit;
            while ((hit = indexOfIgnoreCase(text, q, from)) >= 0) {
                if (hit > from) flow.getChildren().add(plain(text.substring(from, hit), textClass));
                flow.getChildren().add(match(text.substring(hit, hit + q.length()), textClass, q.length() > 40));
                from = hit + q.length();
            }
        }
        if (from < text.length()) flow.getChildren().add(plain(text.substring(from), textClass));
        return flow;
    }

    private static Node plain(String value, String textClass) {
        Text text = new Text(value);
        text.getStyleClass().addAll("flow-text", textClass);
        return text;
    }

    private static Node match(String value, String textClass, boolean longMatch) {
        if (longMatch) { // very long queries would not wrap inside a chip; fall back to coloured text
            Text text = new Text(value);
            text.getStyleClass().addAll("flow-text", textClass);
            text.setStyle("-fx-fill: -tertiary; -fx-font-weight: bold;");
            return text;
        }
        Label chip = new Label(value);
        chip.getStyleClass().addAll("match-chip", textClass + "-hit");
        return chip;
    }
}
