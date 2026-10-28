package com.deepnote.fx;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.function.Consumer;

/** Modern dark modal dialogs: a rounded card centred over the app on a dimmed, transparent overlay stage. */
final class Dialogs {
    private Dialogs() { }

    /** Text prompt (New Note / Rename). The action receives the trimmed, non-blank text. */
    static void prompt(Window owner, String title, String subtitle, String initial, String confirmText, Consumer<String> onConfirm) {
        TextField input = new TextField(initial); input.getStyleClass().add("dialog-input");
        Stage stage = stage(owner);
        Button cancel = button("Cancel", "dialog-secondary", stage::close); cancel.setCancelButton(true);
        Button confirm = button(confirmText, "dialog-primary", () -> {
            String value = input.getText().trim();
            if (!value.isEmpty()) { stage.close(); onConfirm.accept(value); }
        });
        confirm.setDefaultButton(true);
        confirm.disableProperty().bind(input.textProperty().isEmpty());
        show(stage, owner, card(title, subtitle, input, cancel, confirm));
        Platform.runLater(() -> { input.requestFocus(); input.selectAll(); });
    }

    /** Yes/no confirmation; {@code destructive} paints the action red. */
    static void confirm(Window owner, String title, String subtitle, String confirmText, boolean destructive, Runnable onConfirm) {
        Stage stage = stage(owner);
        Button cancel = button("Cancel", "dialog-secondary", stage::close); cancel.setCancelButton(true);
        Button confirm = button(confirmText, destructive ? "dialog-danger" : "dialog-primary", () -> { stage.close(); onConfirm.run(); });
        confirm.setDefaultButton(true);
        show(stage, owner, card(title, subtitle, null, cancel, confirm));
    }

    /** Unsaved-changes prompt: Cancel (nothing happens), Discard, Save. */
    static void unsaved(Window owner, String title, String subtitle, Runnable onSave, Runnable onDiscard) {
        Stage stage = stage(owner);
        Button cancel = button("Cancel", "dialog-secondary", stage::close); cancel.setCancelButton(true);
        Button discard = button("Discard", "dialog-danger", () -> { stage.close(); onDiscard.run(); });
        Button save = button("Save", "dialog-primary", () -> { stage.close(); onSave.run(); });
        save.setDefaultButton(true);
        show(stage, owner, card(title, subtitle, null, cancel, discard, save));
    }

    /** Simple notice with a single OK button (errors). */
    static void message(Window owner, String title, String text) {
        Stage stage = stage(owner);
        Button ok = button("OK", "dialog-primary", stage::close); ok.setDefaultButton(true); ok.setCancelButton(true);
        show(stage, owner, card(title, text, null, ok));
    }

    // ------------------------------------------------------------------ internals

    private static Stage stage(Window owner) {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        if (owner != null) stage.initOwner(owner);
        stage.initModality(owner != null ? Modality.WINDOW_MODAL : Modality.APPLICATION_MODAL);
        return stage;
    }

    private static Button button(String text, String styleClass, Runnable action) {
        Button button = new Button(text); button.getStyleClass().addAll("dialog-button", styleClass);
        button.setOnAction(e -> action.run());
        return button;
    }

    private static VBox card(String title, String subtitle, TextField input, Button... buttons) {
        Label heading = new Label(title); heading.getStyleClass().add("dialog-title");
        Label body = new Label(subtitle); body.getStyleClass().add("dialog-subtitle"); body.setWrapText(true);
        HBox actions = new HBox(10); actions.setAlignment(Pos.CENTER_RIGHT); actions.getChildren().addAll(buttons);
        VBox card = new VBox(8, heading, body);
        if (input != null) { VBox.setMargin(input, new Insets(8, 0, 0, 0)); card.getChildren().add(input); }
        Region gap = new Region(); gap.setMinHeight(10); card.getChildren().addAll(gap, actions);
        card.getStyleClass().add("dialog-card");
        card.setPrefWidth(400); card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return card;
    }

    private static void show(Stage stage, Window owner, VBox card) {
        StackPane backdrop = new StackPane(card); backdrop.getStyleClass().add("dialog-backdrop"); backdrop.setAlignment(Pos.CENTER);
        Scene scene = new Scene(backdrop); scene.setFill(Color.TRANSPARENT);
        if (owner != null && owner.getScene() != null) scene.getStylesheets().setAll(owner.getScene().getStylesheets());
        stage.setScene(scene);
        if (owner != null) { stage.setX(owner.getX()); stage.setY(owner.getY()); stage.setWidth(owner.getWidth()); stage.setHeight(owner.getHeight()); }
        else { Rectangle2D v = Screen.getPrimary().getVisualBounds(); stage.setX(v.getMinX()); stage.setY(v.getMinY()); stage.setWidth(v.getWidth()); stage.setHeight(v.getHeight()); }
        stage.show();
    }
}
