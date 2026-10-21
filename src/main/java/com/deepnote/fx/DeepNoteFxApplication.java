package com.deepnote.fx;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Minimal JavaFX foundation launcher. The complete wireframe migration belongs
 * to a later phase; the existing Swing application remains available meanwhile.
 */
public final class DeepNoteFxApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        Parent root = FXMLLoader.load(DeepNoteFxApplication.class.getResource("/com/deepnote/fx/foundation.fxml"));
        Scene scene = new Scene(root, 960, 620);
        scene.getStylesheets().add(DeepNoteFxApplication.class
                .getResource("/com/deepnote/fx/foundation.css").toExternalForm());
        stage.setTitle("DeepNote — JavaFX Foundation");
        stage.setMinWidth(760);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
