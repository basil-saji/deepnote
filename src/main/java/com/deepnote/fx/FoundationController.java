package com.deepnote.fx;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Controller seam for the smoke-test FXML view and future JavaFX workspace. */
public final class FoundationController {
    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        statusLabel.setText("JavaFX 27 · AtlantaFX 3.0.0");
    }
}
