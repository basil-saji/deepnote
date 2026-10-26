package com.deepnote.fx;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.IOException;

/** JavaFX entry point: an undecorated window whose custom top bar acts as the title bar. */
public final class DeepNoteFxApplication extends Application {
    @Override public void start(Stage stage) throws IOException {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        Parent root = FXMLLoader.load(DeepNoteFxApplication.class.getResource("/com/deepnote/fx/foundation.fxml"));
        Rectangle2D screen = Screen.getPrimary().getVisualBounds(); // excludes the taskbar
        double width = Math.min(1200, screen.getWidth()), height = Math.min(780, screen.getHeight());
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(DeepNoteFxApplication.class.getResource("/com/deepnote/fx/foundation.css").toExternalForm());
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("DeepNote — Workspace");
        stage.setMinWidth(Math.min(860, screen.getWidth())); stage.setMinHeight(Math.min(560, screen.getHeight()));
        stage.setScene(scene);
        stage.setX(screen.getMinX() + (screen.getWidth() - width) / 2);
        stage.setY(screen.getMinY() + (screen.getHeight() - height) / 2);
        stage.show();
    }
    public static void main(String[] args) { launch(args); }
}
