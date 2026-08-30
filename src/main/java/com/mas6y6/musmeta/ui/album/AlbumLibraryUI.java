package com.mas6y6.musmeta.ui.album;

import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.Core;

import javax.swing.*;
import java.awt.*;

public class AlbumLibraryUI extends JScrollPane {
    private static final int GAP = 15;
    private static final int PADDING = 10;

    private final JPanel content;

    public AlbumLibraryUI() {
        super(
                VERTICAL_SCROLLBAR_AS_NEEDED,
                HORIZONTAL_SCROLLBAR_NEVER
        );

        content = new JPanel(new FlowLayout(
                FlowLayout.LEFT,
                GAP,
                GAP
        )) {
            @Override
            public Dimension getPreferredSize() {
                Dimension preferred = new Dimension(super.getPreferredSize());
                if (getParent() instanceof JViewport viewport && viewport.getWidth() > 0) {
                    preferred.width = viewport.getWidth();
                    preferred.height = getLayout().preferredLayoutSize(this).height;
                }
                return preferred;
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
        String artist = album.getArtist();
        if (album.hasDiscs()) {
            artist = artist + "  •  " + album.getDiscs().size() + " discs";
        }
        return new AlbumUI(album.getTitle(), artist);
    }
}