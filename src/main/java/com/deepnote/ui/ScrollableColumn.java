package com.deepnote.ui;

import javax.swing.*;
import java.awt.*;

/** A panel that follows the viewport width, so wrapped text inside it reflows when the window is resized. */
public class ScrollableColumn extends JPanel implements Scrollable {
    private final boolean fillViewportHeight;

    /** @param fillViewportHeight stretch short content to the viewport height (editor) or keep it top-aligned (chat) */
    public ScrollableColumn(LayoutManager layout, boolean fillViewportHeight) {
        super(layout);
        this.fillViewportHeight = fillViewportHeight;
        setOpaque(false);
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

    @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 18; }

    @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? Math.max(40, visible.height - 24) : visible.width;
    }

    @Override public boolean getScrollableTracksViewportWidth() { return true; }

    /** Stretching lets a click anywhere in a short editor column still focus the text area. */
    @Override public boolean getScrollableTracksViewportHeight() {
        return fillViewportHeight && getParent() instanceof JViewport viewport
                && viewport.getHeight() > getPreferredSize().height;
    }
}
