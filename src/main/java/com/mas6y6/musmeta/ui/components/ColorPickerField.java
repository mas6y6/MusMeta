package com.mas6y6.musmeta.ui.components;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;

public class ColorPickerField extends JPanel {
    private final @NonNull JTextField editColorField;
    private final ArrayList<Listener> listeners = new ArrayList<>();

    public interface Listener {
        void colorChanged(Color color);
    }

    private final JPanel colorPreviewPanel = new JPanel() {{
        setPreferredSize(new Dimension(24, 24));
        setBackground(Color.RED);
    }};

    private @NonNull String colorCode = "#FF0000";
    private Color previousColor = Color.RED;

    public ColorPickerField(@NonNull String colorCode) {
        this();
        setColorCode(colorCode);
    }

    public ColorPickerField() {
        super(new BorderLayout(10, 0));

        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        editColorField = new JTextField();
        editColorField.setText(colorCode);

        editColorField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                // Save the current valid color before editing
                previousColor = parseColor(colorCode);
            }

            @Override
            public void focusLost(FocusEvent e) {
                String text = editColorField.getText();

                if (isValidColor(text)) {
                    // Valid color, so save it
                    colorCode = text.toUpperCase();
                    previousColor = parseColor(colorCode);
                    colorPreviewPanel.setBackground(previousColor);

                    fireColorChanged(parseColor(colorCode));
                } else {
                    // Invalid color, revert everything
                    editColorField.setText(toHex(previousColor));
                    colorCode = toHex(previousColor);
                    colorPreviewPanel.setBackground(previousColor);
                }
            }
        });

        JButton selectButton = new JButton("Select Color...");
        selectButton.addActionListener(_ -> {
            Color selectedColor = JColorChooser.showDialog(
                    this,
                    "Select Color",
                    parseColor(colorCode)
            );

            if (selectedColor != null) {
                setColorCode(toHex(selectedColor));
            }
        });

        add(colorPreviewPanel, BorderLayout.WEST);
        add(editColorField, BorderLayout.CENTER);
        add(selectButton, BorderLayout.EAST);
    }

    public @NonNull String getColorCode() {
        return colorCode;
    }

    public void setColorCode(@NonNull String colorCode) {
        if (!isValidColor(colorCode)) {
            throw new IllegalArgumentException(
                    "Invalid color code: " + colorCode
            );
        }

        this.colorCode = colorCode.toUpperCase();

        Color color = parseColor(this.colorCode);

        previousColor = color;
        colorPreviewPanel.setBackground(color);
        editColorField.setText(this.colorCode);
        fireColorChanged(color);
    }

    private boolean isValidColor(@Nullable String value) {
        return value != null && value.matches("^#[0-9A-Fa-f]{6}$");
    }

    private @NonNull Color parseColor(@NonNull String value) {
        return Color.decode(value);
    }

    private @NonNull String toHex(@NonNull Color color) {
        return String.format(
                "#%02X%02X%02X",
                color.getRed(),
                color.getGreen(),
                color.getBlue()
        );
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void fireColorChanged(Color color) {
        for (Listener listener : listeners) {
            listener.colorChanged(color);
        }
    }
}