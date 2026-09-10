package com.mas6y6.musmeta.ui.components;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.ui.components.album.AlbumArtwork;
import com.mas6y6.musmeta.utils.ColorWrapper;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;

public class MusicPlayerPanel extends JPanel {

    private static final int ARTWORK_SIZE = 44;
    private static final int TRANSPORT_SIZE = 30;
    private static final int SEEK_MAX = 1000;

    private static final Icon ICON_PLAY = new FlatSVGIcon(Objects.requireNonNull(MusicPlayerPanel.class.getResource("/play.svg"))).derive(16, 16);
    private static final Icon ICON_PAUSE = new FlatSVGIcon(Objects.requireNonNull(MusicPlayerPanel.class.getResource("/pause.svg"))).derive(16, 16);
    private static final Icon ICON_PREV = new FlatSVGIcon(Objects.requireNonNull(MusicPlayerPanel.class.getResource("/skip-back.svg"))).derive(16, 16);
    private static final Icon ICON_NEXT = new FlatSVGIcon(Objects.requireNonNull(MusicPlayerPanel.class.getResource("/skip-forward.svg"))).derive(16, 16);
    private static final Icon ICON_VOL_NONE = new FlatSVGIcon(Objects.requireNonNull(MusicPlayerPanel.class.getResource("/volume-x.svg"))).derive(14, 14);
    private static final Icon ICON_VOL_LOW = new FlatSVGIcon(Objects.requireNonNull(MusicPlayerPanel.class.getResource("/volume.svg"))).derive(14, 14);
    private static final Icon ICON_VOL_MED = new FlatSVGIcon(Objects.requireNonNull(MusicPlayerPanel.class.getResource("/volume-1.svg"))).derive(14, 14);
    private static final Icon ICON_VOL_HIGH = new FlatSVGIcon(Objects.requireNonNull(MusicPlayerPanel.class.getResource("/volume-2.svg"))).derive(14, 14);

    private AlbumArtwork artwork;
    private JLabel titleLabel;
    private JLabel artistLabel;

    private JButton previousButton;
    private JButton playPauseButton;
    private JButton nextButton;

    private JSlider seekBar;
    private JLabel currentTimeLabel;
    private JLabel totalTimeLabel;

    private JLabel volumeIconLabel;
    private JSlider volumeSlider;

    private boolean playing;
    private Runnable onPrevious;
    private Runnable onNext;
    private Consumer<Boolean> onPlayStateChange;
    private Consumer<Integer> onSeek;
    private Consumer<Integer> onVolumeChange;

    public MusicPlayerPanel() {
        super(new BorderLayout(10, 0));
        setBackground(ColorWrapper.addWrapper(getBackground()).brighter(0.80f));
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        setVisible(Settings.SHOW_MUSIC_PLAYER.get());

        add(trackInfo(), BorderLayout.WEST);
        add(controls(), BorderLayout.CENTER);
        add(volume(), BorderLayout.EAST);
    }

    private JComponent trackInfo() {
        artwork = new AlbumArtwork(8);
        artwork.setPreferredSize(new Dimension(ARTWORK_SIZE, ARTWORK_SIZE));
        artwork.setMaximumSize(artwork.getPreferredSize());

        titleLabel = new JLabel("No track selected");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        artistLabel = new JLabel(" ");
        artistLabel.setFont(artistLabel.getFont().deriveFont(Font.PLAIN, 10f));
        artistLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        artistLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setAlignmentY(Component.CENTER_ALIGNMENT);
        text.setMaximumSize(new Dimension(180, ARTWORK_SIZE));
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(1));
        text.add(artistLabel);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.X_AXIS));
        info.add(artwork);
        info.add(Box.createHorizontalStrut(8));
        info.add(text);

        return info;
    }

    private JComponent controls() {
        previousButton = transportButton(ICON_PREV, "Previous");
        nextButton = transportButton(ICON_NEXT, "Next");
        playPauseButton = transportButton(ICON_PLAY, "Play");
        playPauseButton.addActionListener(e -> setPlaying(!playing));

        previousButton.addActionListener(e -> {
            if (onPrevious != null) {
                onPrevious.run();
            }
        });
        nextButton.addActionListener(e -> {
            if (onNext != null) {
                onNext.run();
            }
        });

        JPanel transport = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        transport.setOpaque(false);
        transport.add(previousButton);
        transport.add(playPauseButton);
        transport.add(nextButton);

        seekBar = new JSlider(0, SEEK_MAX, 0);
        seekBar.setOpaque(false);
        seekBar.setFocusable(false);
        seekBar.setPreferredSize(new Dimension(300, 20));
        seekBar.addChangeListener(e -> {
            if (!seekBar.getValueIsAdjusting() && onSeek != null) {
                onSeek.accept(seekBar.getValue());
            }
        });

        currentTimeLabel = timeLabel("0:00");
        totalTimeLabel = timeLabel("0:00");

        JPanel seek = new JPanel(new BorderLayout(6, 0));
        seek.setOpaque(false);
        seek.add(currentTimeLabel, BorderLayout.WEST);
        seek.add(seekBar, BorderLayout.CENTER);
        seek.add(totalTimeLabel, BorderLayout.EAST);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(transport);
        center.add(Box.createVerticalStrut(2));
        center.add(seek);

        return center;
    }

    private JComponent volume() {
        volumeIconLabel = new JLabel(ICON_VOL_HIGH);
        volumeIconLabel.setPreferredSize(new Dimension(16, 16));

        volumeSlider = new JSlider(0, 100, 80);
        volumeSlider.setOpaque(false);
        volumeSlider.setFocusable(false);
        volumeSlider.setPreferredSize(new Dimension(90, 20));
        volumeSlider.setMaximumSize(volumeSlider.getPreferredSize());
        volumeSlider.addChangeListener(e -> {
            updateVolumeIcon();
            if (!volumeSlider.getValueIsAdjusting() && onVolumeChange != null) {
                onVolumeChange.accept(volumeSlider.getValue());
            }
        });

        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setOpaque(false);
        panel.add(volumeIconLabel, BorderLayout.WEST);
        panel.add(volumeSlider, BorderLayout.CENTER);

        return panel;
    }

    private void updateVolumeIcon() {
        int vol = volumeSlider.getValue();
        Icon icon;
        if (vol == 0) {
            icon = ICON_VOL_NONE;
        } else if (vol <= 33) {
            icon = ICON_VOL_LOW;
        } else if (vol <= 66) {
            icon = ICON_VOL_MED;
        } else {
            icon = ICON_VOL_HIGH;
        }
        volumeIconLabel.setIcon(icon);
    }

    private JButton transportButton(Icon icon, String toolTip) {
        JButton button = new JButton(icon);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(toolTip);
        button.setPreferredSize(new Dimension(TRANSPORT_SIZE, TRANSPORT_SIZE));
        button.setMaximumSize(button.getPreferredSize());
        return button;
    }

    private static JLabel timeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 10f));
        label.setForeground(UIManager.getColor("Label.disabledForeground"));
        label.setPreferredSize(new Dimension(36, 16));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    public void setTrack(Song song) {
        if (song == null) {
            clearTrack();
            return;
        }
        setTrackInfo(song.getTitle(), song.getArtist());
        artwork.setArtwork(song.getArtworkImage());
    }

    public void setTrackInfo(String title, String artist) {
        titleLabel.setText(title == null || title.isBlank() ? "Untitled" : title);
        artistLabel.setText(artist == null || artist.isBlank() ? " " : artist);
    }

    public void clearTrack() {
        setTrackInfo("No track selected", " ");
        artwork.setArtwork(null);
        setPlaying(false);
        setTime(0, 0);
    }

    public void setPlaying(boolean playing) {
        if (this.playing == playing) {
            return;
        }
        this.playing = playing;
        playPauseButton.setIcon(playing ? ICON_PAUSE : ICON_PLAY);
        playPauseButton.setToolTipText(playing ? "Pause" : "Play");
        if (onPlayStateChange != null) {
            onPlayStateChange.accept(playing);
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setTime(long seconds, long totalSeconds) {
        currentTimeLabel.setText(formatTime((int) seconds));
        totalTimeLabel.setText(formatTime((int) totalSeconds));
        if (totalSeconds > 0 && !seekBar.getValueIsAdjusting()) {
            seekBar.setValue((int) Math.round(seconds * SEEK_MAX / (double) totalSeconds));
        }
    }

    public void setVolume(int percent) {
        volumeSlider.setValue(Math.clamp(percent, 0, 100));
        updateVolumeIcon();
    }

    public int getVolume() {
        return volumeSlider.getValue();
    }

    public void setOnPrevious(Runnable onPrevious) {
        this.onPrevious = onPrevious;
    }

    public void setOnNext(Runnable onNext) {
        this.onNext = onNext;
    }

    public void setOnPlayStateChange(Consumer<Boolean> onPlayStateChange) {
        this.onPlayStateChange = onPlayStateChange;
    }

    public void setOnSeek(Consumer<Integer> onSeek) {
        this.onSeek = onSeek;
    }

    public void setOnVolumeChange(Consumer<Integer> onVolumeChange) {
        this.onVolumeChange = onVolumeChange;
    }

    private static String formatTime(int totalSeconds) {
        if (totalSeconds <= 0) {
            return "0:00";
        }
        return (totalSeconds / 60) + ":" + String.format("%02d", totalSeconds % 60);
    }
}
