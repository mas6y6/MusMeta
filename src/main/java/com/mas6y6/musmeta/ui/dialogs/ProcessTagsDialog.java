package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.Library;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.ui.MainWindow;
import com.mas6y6.musmeta.ui.dialogs.base.ProcessingDialog;
import org.jaudiotagger.tag.FieldKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProcessTagsDialog extends ProcessingDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessTagsDialog.class);

    public enum ArtworkAction {
        KEEP,
        REPLACE,
        REMOVE
    }

    public record SongTagUpdate(
            Song song,
            Map<FieldKey, String> fieldsToSet,
            Set<FieldKey> fieldsToDelete,
            Boolean compilation,
            ArtworkAction artworkAction,
            byte[] artworkData,
            String artworkMimeType
    ) {
    }

    private final List<SongTagUpdate> updates;
    private final Album targetAlbum;
    private final String oldAlbumTitle;

    public ProcessTagsDialog(Window owner, List<SongTagUpdate> updates) {
        this(owner, updates, null, null);
    }

    public ProcessTagsDialog(Window owner, List<SongTagUpdate> updates, Album targetAlbum, String oldAlbumTitle) {
        super(owner, "Applying Tags", ProgressMode.DETERMINATE);
        this.updates = updates != null ? updates : List.of();
        this.targetAlbum = targetAlbum;
        this.oldAlbumTitle = oldAlbumTitle;
    }

    @Override
    protected boolean process() throws Exception {
        if (updates.isEmpty()) {
            return true;
        }

        updateProgress("Applying tags", 0, "Processing audio files...");

        int total = updates.size();
        for (int i = 0; i < total; i++) {
            SongTagUpdate update = updates.get(i);
            Song song = update.song();

            int percent = (int) Math.round((i * 100.0) / total);
            updateProgress(
                    "Applying tags " + (i + 1) + " of " + total,
                    percent,
                    song.getAudioFile() != null && song.getAudioFile().getFile() != null
                            ? song.getAudioFile().getFile().getName()
                            : song.getTitle()
            );

            try {
                if (update.fieldsToSet() != null) {
                    for (Map.Entry<FieldKey, String> entry : update.fieldsToSet().entrySet()) {
                        song.setTagField(entry.getKey(), entry.getValue());
                    }
                }

                if (update.fieldsToDelete() != null) {
                    for (FieldKey key : update.fieldsToDelete()) {
                        song.setTagField(key, null);
                    }
                }

                if (update.compilation() != null) {
                    song.setCompilation(update.compilation());
                }

                if (update.artworkAction() == ArtworkAction.REPLACE) {
                    song.setArtwork(update.artworkData(), update.artworkMimeType());
                } else if (update.artworkAction() == ArtworkAction.REMOVE) {
                    song.deleteArtwork();
                }

                song.saveToFile();
            } catch (Exception e) {
                LOGGER.error("Failed to write tags for song {}", song.getTitle(), e);
            }
        }

        updateProgress("Finalizing", 95, "Updating library...");

        if (targetAlbum != null) {
            if (!updates.isEmpty()) {
                SongTagUpdate first = updates.get(0);
                if (first.artworkAction() == ArtworkAction.REPLACE && first.artworkData() != null) {
                    targetAlbum.setArtworkBytes(first.artworkData());
                } else if (first.artworkAction() == ArtworkAction.REMOVE) {
                    targetAlbum.removeArtwork();
                }
            }

            if (oldAlbumTitle != null && !oldAlbumTitle.equalsIgnoreCase(targetAlbum.getTitle())) {
                Library.getInstance().renameAlbum(targetAlbum, targetAlbum.getTitle());
            }

            for (SongTagUpdate update : updates) {
                Library.getInstance().reorganizeSong(update.song());
            }
        } else {
            for (SongTagUpdate update : updates) {
                Library.getInstance().reorganizeSong(update.song());
            }
        }

        Library.getInstance().save();

        SwingUtilities.invokeLater(() -> {
            if (targetAlbum != null) {
                MainWindow.INSTANCE.updateAlbumTabs(targetAlbum, oldAlbumTitle);
            } else {
                MainWindow.INSTANCE.refreshAllDetailTabs();
            }
        });

        updateProgress("Completed", 100, "All tags applied successfully");
        return true;
    }
}
