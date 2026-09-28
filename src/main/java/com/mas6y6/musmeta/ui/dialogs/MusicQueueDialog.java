package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.musicplayer.MusicPlayer;
import com.mas6y6.musmeta.ui.components.PlayerQueueTableModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MusicQueueDialog extends JDialog {

    private static final Dimension DIALOG_SIZE = new Dimension(620, 420);

    private final MusicPlayer player = MusicPlayer.getInstance();

    private final PlayerQueueTableModel queueModel = new PlayerQueueTableModel();

    private final JTable table = new JTable(queueModel);

    private final JLabel summary = new JLabel();

    private final JButton playButton = new JButton("Play");
    private final JButton upButton = new JButton("Up");
    private final JButton downButton = new JButton("Down");
    private final JButton removeButton = new JButton("Remove");
    private final JButton clearButton = new JButton("Clear");
    private final JButton closeButton = new JButton("Close");

    private final MusicPlayer.Listener listener = new MusicPlayer.Listener() {
        @Override
        public void queueChanged() {
            runOnEDT(MusicQueueDialog.this::refresh);
        }

        @Override
        public void songChanged(Song song) {
            runOnEDT(MusicQueueDialog.this::refresh);
        }

        @Override
        public void stateUpdated(
                boolean isPlaying,
                boolean isPaused,
                boolean isStopped
        ) {
            runOnEDT(MusicQueueDialog.this::refresh);
        }
    };

    public MusicQueueDialog(Window owner) {
        super(owner, "Queue", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setSize(DIALOG_SIZE);
        setMinimumSize(new Dimension(420, 280));
        setLocationRelativeTo(owner != null ? owner : this);

        queueModel.reload();

        setContentPane(content());

        player.addListener(listener);
    }

    //region Layout

    private JComponent content() {
        JPanel page = new JPanel(new BorderLayout(10, 12));
        page.setBorder(new EmptyBorder(20, 25, 20, 25));
        page.add(header(), BorderLayout.NORTH);
        page.add(listPanel(), BorderLayout.CENTER);
        page.add(footer(), BorderLayout.SOUTH);
        return page;
    }

    private JComponent header() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Queue");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);

        header.add(Box.createVerticalStrut(8));

        return header;
    }

    private JComponent listPanel() {
        table.setRowHeight(26);
        table.setShowVerticalLines(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(44);
        table.getColumnModel().getColumn(0).setHeaderRenderer(
                tooltipHeaderRenderer(
                        "Type a new number to move the song to that position"
                )
        );
        table.getColumnModel().getColumn(1).setPreferredWidth(240);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);
        table.getColumnModel().getColumn(3).setPreferredWidth(170);

        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel()
                    .getColumn(column)
                    .setCellRenderer(playingRowRenderer());
        }

        table.getSelectionModel().addListSelectionListener(
                e -> {
                    if (!e.getValueIsAdjusting()) {
                        updateControls();
                    }
                }
        );

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2
                        && SwingUtilities.isLeftMouseButton(e)) {
                    playSelected();
                }
            }
        });

        table.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                "playSelected"
        );
        table.getActionMap().put("playSelected", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                playSelected();
            }
        });

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(listButtons(), BorderLayout.SOUTH);
        return panel;
    }

    private JComponent listButtons() {
        JPanel panel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 0)
        );
        panel.setOpaque(false);

        playButton.setToolTipText("Start playing the selected song");
        playButton.addActionListener(e -> playSelected());
        panel.add(playButton);

        upButton.setToolTipText("Move the selected song up one position");
        upButton.addActionListener(e -> moveSelected(-1));
        panel.add(upButton);

        downButton.setToolTipText("Move the selected song down one position");
        downButton.addActionListener(e -> moveSelected(1));
        panel.add(downButton);

        panel.add(Box.createHorizontalStrut(12));

        removeButton.setToolTipText("Take the selected song off the queue");
        removeButton.addActionListener(e -> removeSelected());
        panel.add(removeButton);

        clearButton.setToolTipText("Stop playback and empty the queue");
        clearButton.addActionListener(e -> clearQueue());
        panel.add(clearButton);

        updateControls();

        return panel;
    }

    private JComponent footer() {
        summary.setFont(summary.getFont().deriveFont(Font.PLAIN, 11f));
        summary.setForeground(
                UIManager.getColor("Label.disabledForeground")
        );

        closeButton.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(closeButton);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(closeButton);

        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.add(summary, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    private TableCellRenderer playingRowRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                Component component = super.getTableCellRendererComponent(
                        table,
                        value,
                        isSelected,
                        hasFocus,
                        row,
                        column
                );

                if (!isSelected) {
                    component.setFont(
                            component.getFont().deriveFont(isPlayingRow(row)
                                    ? Font.BOLD
                                    : Font.PLAIN)
                    );
                }

                return component;
            }
        };
    }

    private static TableCellRenderer tooltipHeaderRenderer(String tooltip) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                Component component = super.getTableCellRendererComponent(
                        table,
                        value,
                        isSelected,
                        hasFocus,
                        row,
                        column
                );

                setToolTipText(tooltip);

                return component;
            }
        };
    }

    //endregion

    //region Queue editing

    private void playSelected() {
        int row = selectedRow();

        if (row < 0) {
            return;
        }

        player.skipTo(row);
    }

    private void moveSelected(int offset) {
        int row = selectedRow();

        if (row < 0) {
            return;
        }

        int target = row + offset;

        if (target < 0 || target >= table.getRowCount()) {
            return;
        }

        queueModel.moveRow(row, target);

        selectRow(target);
    }

    private void removeSelected() {
        int row = selectedRow();

        if (row < 0) {
            return;
        }

        queueModel.remove(row);

        if (table.getRowCount() == 0) {
            table.clearSelection();
        } else {
            selectRow(Math.min(row, table.getRowCount() - 1));
        }
    }

    /**
     * Stops playback before emptying the queue, since the player refuses to
     * drop songs that are still being played.
     */
    private void clearQueue() {
        player.stop();
        queueModel.clear();
    }

    //endregion

    private void refresh() {
        queueModel.reload();

        table.repaint();
        updateControls();
    }

    private void updateControls() {
        int row = selectedRow();
        boolean selected = row >= 0;

        playButton.setEnabled(selected);
        removeButton.setEnabled(selected);
        upButton.setEnabled(selected && row > 0);
        downButton.setEnabled(selected && row < table.getRowCount() - 1);

        // The player will not empty a queue that is still playing.
        clearButton.setEnabled(
                table.getRowCount() > 0 && !player.isPlaying()
        );

        summary.setText(summaryText());
    }

    private String summaryText() {
        int count = table.getRowCount();

        if (count == 0) {
            return "The queue is empty. Songs you play are listed here.";
        }

        int playing = playingRow();

        if (playing < 0) {
            return count == 1
                    ? "1 song queued."
                    : count + " songs queued.";
        }

        return "Song " + (playing + 1) + " of " + count + ": "
                + player.getQueue().get(playing).getTitle();
    }

    private int selectedRow() {
        int viewRow = table.getSelectedRow();

        return viewRow < 0
                ? -1
                : table.convertRowIndexToModel(viewRow);
    }

    private void selectRow(int row) {
        int viewRow = table.convertRowIndexToView(row);

        if (viewRow < 0) {
            return;
        }

        table.setRowSelectionInterval(viewRow, viewRow);
    }

    private int playingRow() {
        int index = player.isStopped()
                ? -1
                : player.getCurrentSongIndex();

        return index < 0 || index >= player.getQueue().size()
                ? -1
                : index;
    }

    private boolean isPlayingRow(int viewRow) {
        int row = table.convertRowIndexToModel(viewRow);

        return row >= 0 && row == playingRow();
    }

    @Override
    public void dispose() {
        player.removeListener(listener);

        super.dispose();
    }

    private static void runOnEDT(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }
}
