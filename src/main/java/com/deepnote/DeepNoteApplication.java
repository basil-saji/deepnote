package com.deepnote;

import com.deepnote.config.AppConfig;
import com.deepnote.ui.MainWindow;
import com.deepnote.ui.DeepNoteTheme;

import javax.swing.*;

/** Application entry point. */
public final class DeepNoteApplication {
    private DeepNoteApplication() { }

    public static void main(String[] args) {
        DeepNoteTheme.install();
        SwingUtilities.invokeLater(() -> new MainWindow(AppConfig.load()).setVisible(true));
    }
}
