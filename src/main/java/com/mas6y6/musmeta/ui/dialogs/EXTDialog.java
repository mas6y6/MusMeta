package com.mas6y6.musmeta.ui.dialogs;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.SystemInfo;

import javax.swing.*;
import java.awt.*;

/**
 * JOptionPane helpers that preserve the active look and feel while removing
 * the native macOS title bar from dialogs.
 */
public class EXTDialog extends JOptionPane {
    private EXTDialog() {
    }

    public static void showMessageDialog(Component parentComponent, Object message,
                                         String title, int messageType) {
        JOptionPane pane = new JOptionPane(message, messageType, DEFAULT_OPTION);
        showDialog(pane, parentComponent, title);
    }

    public static int showConfirmDialog(Component parentComponent, Object message,
                                        String title, int optionType) {
        return showConfirmDialog(parentComponent, message, title, optionType,
                QUESTION_MESSAGE);
    }

    public static int showConfirmDialog(Component parentComponent, Object message,
                                        String title, int optionType, int messageType) {
        JOptionPane pane = new JOptionPane(message, messageType, optionType);
        return valueOrClosedOption(pane, showDialog(pane, parentComponent, title));
    }

    public static int showOptionDialog(Component parentComponent, Object message,
                                       String title, int optionType, int messageType,
                                       Icon icon, Object[] options, Object initialValue) {
        JOptionPane pane = new JOptionPane(message, messageType, optionType, icon,
                options, initialValue);
        Object value = showDialog(pane, parentComponent, title);

        if (value == UNINITIALIZED_VALUE) {
            return CLOSED_OPTION;
        }
        if (options == null) {
            return value instanceof Integer ? (Integer) value : CLOSED_OPTION;
        }
        for (int index = 0; index < options.length; index++) {
            if (options[index].equals(value)) {
                return index;
            }
        }
        return CLOSED_OPTION;
    }

    private static Object showDialog(JOptionPane pane, Component parentComponent,
                                     String title) {
        JDialog dialog = pane.createDialog(parentComponent, title);
        Toolkit.getDefaultToolkit().beep();
        try {
            removeMacTitleBar(dialog);
            dialog.setVisible(true);
            return pane.getValue();
        } finally {
            dialog.dispose();
        }
    }

    private static int valueOrClosedOption(JOptionPane pane, Object value) {
        return value == UNINITIALIZED_VALUE || !(value instanceof Integer)
                ? CLOSED_OPTION
                : (Integer) value;
    }

    /**
     * JOptionPane#createDialog packs the dialog, which creates its native
     * window peer. Dispose that peer before changing its decoration state;
     * the existing content, size, modality, and active look and feel remain.
     */
    private static void removeMacTitleBar(JDialog dialog) {
        //TODO: remove dialog titlebar

        dialog.getRootPane().putClientProperty(FlatClientProperties.USE_WINDOW_DECORATIONS, true);
        dialog.getRootPane().putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT, true);
        dialog.getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_CLOSE, false);
        dialog.getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_MAXIMIZE, false);
        dialog.getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_ICONIFFY, false);

        /*if (SystemInfo.isMacOS) {
            if (dialog.isDisplayable()) {
                dialog.dispose();
            }
            dialog.getRootPane().putClientProperty();
        }*/
    }
}
