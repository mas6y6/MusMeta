package com.mas6y6.musmeta.ui.album;

import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.Core;
import com.mas6y6.musmeta.core.Library;

import javax.swing.*;
import java.awt.*;

public class LibraryUI extends JScrollPane {
    private static final int GAP = 15;
    private static final int PADDING = 10;

    private final JPanel content;

    public LibraryUI() {
        super(
                VERTICAL_SCROLLBAR_AS_NEEDED,
                HORIZONTAL_SCROLLBAR_NEVER
        );

        WrapLayout wrapLayout = new WrapLayout();

        content = new JPanel(wrapLayout) {
            @Override
            public Dimension getPreferredSize() {
                int width = 400;
                if (getParent() instanceof JViewport viewport
                        && viewport.getWidth() > 0) {
                    width = viewport.getWidth();
                }
                return wrapLayout.sizeForWidth(this, width);
            }
        };

        content.setBorder(BorderFactory.createEmptyBorder(
                PADDING,
                PADDING,
                PADDING,
                PADDING
        ));

        setViewportView(content);
        setViewportBorder(null);
        setBorder(null);

        getVerticalScrollBar().setUnitIncrement(16);

        Library.CONFIG.addListener(e ->
                SwingUtilities.invokeLater(this::refresh)
        );

        refresh();
    }

    public void refresh() {
        content.removeAll();

        content.add(new AddAlbumUI());

        for (Album album : Core.getAlbums()) {
            content.add(albumCard(album));
        }

        content.revalidate();
        content.repaint();
    }

    private Component albumCard(Album album) {
        return new AlbumUI(album, album.getArtworkImage());
    }

    private static final class WrapLayout implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            int width = parent.getWidth() > 0
                    ? parent.getWidth()
                    : Integer.MAX_VALUE;
            return sizeForWidth(parent, width);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return sizeForWidth(parent, Integer.MAX_VALUE);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            int available = parent.getWidth() - insets.left - insets.right;

            int x = insets.left;
            int y = insets.top;
            int rowHeight = 0;

            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) {
                    continue;
                }

                Dimension d = c.getPreferredSize();

                if (x + d.width > insets.left + available && x > insets.left) {
                    x = insets.left;
                    y += rowHeight + GAP;
                    rowHeight = 0;
                }

                c.setBounds(x, y, d.width, d.height);
                x += d.width + GAP;
                rowHeight = Math.max(rowHeight, d.height);
            }
        }

        private Dimension sizeForWidth(Container parent, int width) {
            Insets insets = parent.getInsets();

            int available = Math.max(0, width - insets.left - insets.right);

            int rowWidth = 0;
            int rowHeight = 0;
            int totalHeight = insets.top;

            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) {
                    continue;
                }

                Dimension d = c.getPreferredSize();

                if (rowWidth + d.width > available && rowWidth > 0) {
                    totalHeight += rowHeight + GAP;
                    rowWidth = 0;
                    rowHeight = 0;
                }

                rowWidth += d.width + GAP;
                rowHeight = Math.max(rowHeight, d.height);
            }

            totalHeight += rowHeight + insets.bottom;

            return new Dimension(width, totalHeight);
        }
    }
}
