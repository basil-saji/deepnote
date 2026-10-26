package com.deepnote.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minimal Markdown-to-JavaFX renderer: headings, **bold**, `code`, fenced code blocks, bullet and numbered lists. */
final class MarkdownView {
    private static final Pattern INLINE = Pattern.compile("\\*\\*(.+?)\\*\\*|`([^`]+)`");
    private static final Pattern HEADING = Pattern.compile("^\\s{0,3}(#{1,6})\\s+(.*)$");
    private static final Pattern BULLET = Pattern.compile("^(\\s*)[-*+]\\s+(.*)$");
    private static final Pattern NUMBERED = Pattern.compile("^(\\s*)(\\d+)[.)]\\s+(.*)$");

    private MarkdownView() { }

    static VBox render(String markdown) {
        VBox box = new VBox(6); box.getStyleClass().add("md-root");
        List<String> paragraph = new ArrayList<>();
        String[] lines = markdown.replace("\r\n", "\n").split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.strip().startsWith("```")) {
                flush(box, paragraph);
                StringBuilder code = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].strip().startsWith("```")) { if (code.length() > 0) code.append('\n'); code.append(lines[i]); i++; }
                box.getChildren().add(codeBlock(code.toString()));
                continue;
            }
            Matcher heading = HEADING.matcher(line), bullet = BULLET.matcher(line), numbered = NUMBERED.matcher(line);
            if (line.isBlank()) flush(box, paragraph);
            else if (heading.matches()) { flush(box, paragraph); box.getChildren().add(inline(heading.group(2).strip(), heading.group(1).length() == 1 ? "md-h1" : "md-h2")); }
            else if (bullet.matches()) { flush(box, paragraph); box.getChildren().add(listItem("•", bullet.group(1).length(), bullet.group(2))); }
            else if (numbered.matches()) { flush(box, paragraph); box.getChildren().add(listItem(numbered.group(2) + ".", numbered.group(1).length(), numbered.group(3))); }
            else paragraph.add(line.strip());
        }
        flush(box, paragraph);
        if (box.getChildren().isEmpty()) box.getChildren().add(inline(markdown, "md-text"));
        return box;
    }

    private static void flush(VBox box, List<String> paragraph) {
        if (paragraph.isEmpty()) return;
        box.getChildren().add(inline(String.join("\n", paragraph), "md-text"));
        paragraph.clear();
    }

    private static Node listItem(String marker, int indentSpaces, String text) {
        Label mark = new Label(marker); mark.getStyleClass().add("md-marker"); mark.setMinWidth(18); mark.setAlignment(Pos.TOP_RIGHT);
        TextFlow flow = inline(text, "md-text"); HBox.setHgrow(flow, Priority.ALWAYS);
        HBox row = new HBox(6, mark, flow); row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(0, 0, 0, Math.min(indentSpaces, 8) * 5));
        return row;
    }

    private static Node codeBlock(String code) {
        Label label = new Label(code); label.getStyleClass().add("md-code-text"); label.setWrapText(true); label.setMaxWidth(Double.MAX_VALUE);
        VBox block = new VBox(label); block.getStyleClass().add("md-code-block");
        return block;
    }

    /** Inline spans: **bold** and `code`; everything else is plain text. */
    static TextFlow inline(String text, String baseClass) {
        TextFlow flow = new TextFlow(); flow.setMinWidth(0); flow.setMaxWidth(Double.MAX_VALUE);
        Matcher m = INLINE.matcher(text);
        int from = 0;
        while (m.find()) {
            if (m.start() > from) flow.getChildren().add(span(text.substring(from, m.start()), baseClass));
            if (m.group(1) != null) flow.getChildren().add(span(m.group(1), baseClass, "md-bold"));
            else { Label code = new Label(m.group(2)); code.getStyleClass().add("md-inline-code"); flow.getChildren().add(code); }
            from = m.end();
        }
        if (from < text.length()) flow.getChildren().add(span(text.substring(from), baseClass));
        return flow;
    }

    private static Text span(String value, String... classes) {
        Text text = new Text(value); text.getStyleClass().addAll(classes);
        return text;
    }
}
