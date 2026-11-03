package com.mas6y6.musmeta.utils;


import javax.swing.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Smooth inertial mouse-wheel scrolling for {@link JScrollPane}s.
 * <p>
 * Swing/FlatLaf scroll wheel handling is replaced so that plain mouse wheel
 * "notch" events are animated with a velocity + friction model instead of
 * jumping by fixed unit increments.
 * <p>
 * All other wheel events (trackpads and high-resolution wheels, shift/ctrl +
 * wheel, ...) are delegated back to the look-and-feel's original wheel
 * listener, because FlatLaf already animates precise/trackpad scrolling
 * itself (see {@code ScrollPane.smoothScrolling}). Without delegation those
 * events would be handled twice once by FlatLaf and once here.
 * <p>
 * While an animation is running, external changes to the scrollbar (user
 * dragging the scrollbar, programmatic scrolling, ...) immediately stop the
 * animation and resync its state, so animation and scrollbar never fight
 * over the scroll position.
 */
public final class SmoothScroll {

    private static final int FRAME_RATE = 16;

    private static final double SCROLL_SPEED = 8.0;
    private static final double FRICTION = 6.0;
    private static final double STOP_THRESHOLD = 3.0;

    private SmoothScroll() {
    }

    public static void enable(JScrollPane scrollPane) {
        // Detach the look-and-feel's wheel listener so each wheel event is
        // handled exactly once. It is kept for delegation below.
        MouseWheelListener[] uiListeners = scrollPane.getMouseWheelListeners();
        for (MouseWheelListener uiListener : uiListeners) {
            scrollPane.removeMouseWheelListener(uiListener);
        }

        JScrollBar bar = scrollPane.getVerticalScrollBar();

        final double[] position = {bar.getValue()};
        final double[] velocity = {0};

        final int[] lastValue = {bar.getValue()};
        final long[] lastFrame = {System.nanoTime()};

        // true while the animation timer itself updates the scrollbar, so the
        // adjustment listener below can distinguish our own updates and only
        // resync on external scrollbar changes.
        final boolean[] applyingScroll = {false};

        Timer timer = new Timer(FRAME_RATE, e -> {
            long now = System.nanoTime();

            double deltaTime = (now - lastFrame[0]) / 1_000_000_000.0;
            lastFrame[0] = now;

            deltaTime = Math.min(deltaTime, 0.05);

            if (deltaTime <= 0) {
                return;
            }

            position[0] += velocity[0] * deltaTime;

            int min = bar.getMinimum();
            int max = bar.getMaximum() - bar.getVisibleAmount();

            position[0] = Math.clamp(position[0], min, max);

            int value = (int) Math.round(position[0]);

            if (value != lastValue[0]) {
                applyingScroll[0] = true;
                try {
                    bar.setValue(value);
                } finally {
                    applyingScroll[0] = false;
                }
                lastValue[0] = value;
            }

            velocity[0] *= Math.exp(-FRICTION * deltaTime);

            if (Math.abs(velocity[0]) < STOP_THRESHOLD) {
                velocity[0] = 0;
                position[0] = bar.getValue();
                lastValue[0] = bar.getValue();
                lastFrame[0] = System.nanoTime();
                ((Timer) e.getSource()).stop();
            }
        });

        // If the scrollbar is moved externally (user drag, programmatic
        // scrolling, ...), stop the animation and resync to the new position.
        bar.addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (applyingScroll[0] || !timer.isRunning()) {
                    return;
                }

                velocity[0] = 0;
                position[0] = e.getValue();
                lastValue[0] = e.getValue();
                timer.stop();
            }
        });

        scrollPane.addMouseWheelListener((MouseWheelEvent e) -> {
            // Plain mouse wheel notch scroll with no modifiers -> animate.
            // Anything else (trackpad / hi-res wheel, shift, ctrl, ...) is
            // delegated to the look-and-feel's original listener.
            boolean plainNotchScroll =
                    e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL
                    && e.getWheelRotation() != 0
                    && e.getPreciseWheelRotation() == e.getWheelRotation()
                    && !e.isShiftDown()
                    && !e.isControlDown();

            if (!plainNotchScroll
                    || !scrollPane.isEnabled()
                    || !scrollPane.isWheelScrollingEnabled()) {
                for (MouseWheelListener uiListener : uiListeners) {
                    uiListener.mouseWheelMoved(e);
                }
                return;
            }

            // Nothing to scroll vertically: let the original handler deal
            // with the event (e.g. it scrolls horizontally instead).
            int min = bar.getMinimum();
            int max = bar.getMaximum() - bar.getVisibleAmount();
            if (max <= min) {
                for (MouseWheelListener uiListener : uiListeners) {
                    uiListener.mouseWheelMoved(e);
                }
                return;
            }

            e.consume();

            velocity[0] += e.getPreciseWheelRotation() * SCROLL_SPEED * 60;

            if (!timer.isRunning()) {
                position[0] = bar.getValue();
                lastValue[0] = bar.getValue();
                lastFrame[0] = System.nanoTime();
                timer.start();
            }
        });
    }
}
