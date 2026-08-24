package com.mas6y6.musmeta.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class RoundedLabel extends JLabel {

    private final int radius;

    public RoundedLabel(Icon icon, int radius) {
        super(icon, SwingConstants.CENTER);
        this.radius = radius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        // Clip the label contents to rounded corners
        g2.setClip(
                new RoundRectangle2D.Float(
                        0,
                        0,
                        getWidth(),
                        getHeight(),
                        radius,
                        radius
                )
        );

        super.paintComponent(g2);

        g2.dispose();
    }
}