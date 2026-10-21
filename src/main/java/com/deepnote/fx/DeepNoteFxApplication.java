package com.deepnote.fx;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/** JavaFX entry point for the migrated DeepNote workspace. */
public final class DeepNoteFxApplication extends Application {
    @Override public void start(Stage stage) throws IOException {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        Parent root = FXMLLoader.load(DeepNoteFxApplication.class.getResource("/com/deepnote/fx/foundation.fxml"));
        Scene scene = new Scene(root, 1200, 780);
        scene.getStylesheets().add(DeepNoteFxApplication.class.getResource("/com/deepnote/fx/foundation.css").toExternalForm());
        stage.setTitle("DeepNote — Workspace");
        stage.setMinWidth(860); stage.setMinHeight(560); stage.setScene(scene); stage.show();
    }
    public static void main(String[] args) { launch(args); }
}
