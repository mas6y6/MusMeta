package com.mas6y6.musmeta;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CrashHandler {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /** Prevents the application from continuing after its first fatal error. */
    private static final AtomicBoolean CRASHING = new AtomicBoolean();
    private static final CountDownLatch CRASH_DIALOG_CLOSED = new CountDownLatch(1);

    private CrashHandler() {
    }

    public static void handle(Thread thread, Throwable throwable) {
        if (!CRASHING.compareAndSet(false, true)) {
            // Do not block the EDT: it is responsible for processing the
            // already-open modal dialog's close event.
            if (!SwingUtilities.isEventDispatchThread()) {
                waitForTermination();
            }
            return;
        }

        freezeApplicationWindows();

        Path crashFile = null;

        try {
            Path crashDirectory = Path.of(
                    System.getProperty("user.home"),
                    ".musmeta",
                    "crash-reports"
            );

            Files.createDirectories(crashDirectory);

            String timestamp = LocalDateTime.now().format(FORMAT);

            crashFile = crashDirectory.resolve(
                    "crash-" + timestamp + ".log"
            );

            try (PrintWriter writer = new PrintWriter(
                    Files.newBufferedWriter(crashFile))) {

                writer.println("---- MusMeta Crash Report ----");
                writer.println();
                writer.println("Time: " + LocalDateTime.now());
                writer.println("Thread: " + thread.getName());
                writer.println("Java: " + System.getProperty("java.version"));
                writer.println("OS: " + System.getProperty("os.name"));
                writer.println("OS Version: " + System.getProperty("os.version"));
                writer.println("Architecture: " + System.getProperty("os.arch"));
                writer.println();

                writer.println("Exception:");
                throwable.printStackTrace(writer);
            }

            System.err.println(
                    "MusMeta crashed. Crash report written to: "
                            + crashFile.toAbsolutePath()
            );

            Reader reader = Files.newBufferedReader(crashFile);
            reader.readAllLines().forEach(System.err::println);

        } catch (IOException exception) {
            System.err.println("Failed to write crash report.");
            exception.printStackTrace();
        }

        Path finalCrashFile = crashFile;

        Runnable showDialog = () -> {
            String message;

            if (finalCrashFile != null) {
                message = """
                MusMeta has encountered an unexpected error and needs to close.

                A crash report has been saved to:

                %s

                Please include this file when reporting the issue.
                """.formatted(finalCrashFile.toAbsolutePath());
            } else {
                message = """
                MusMeta has encountered an unexpected error and needs to close.

                Unfortunately, MusMeta was unable to create a crash report.
                """;
            }

            JTextArea crashText = new JTextArea();

            try {
                if (finalCrashFile != null) {
                    crashText.setText(Files.readString(finalCrashFile));
                } else {
                    crashText.setText(
                            "Crash report could not be created."
                    );
                }
            } catch (IOException exception) {
                crashText.setText(
                        "Failed to read crash report:\n\n" + exception
                );
            }

            crashText.setEditable(false);
            crashText.setLineWrap(false);
            crashText.setWrapStyleWord(false);

            JScrollPane scrollPane = new JScrollPane(crashText);
            scrollPane.setPreferredSize(new Dimension(700, 400));

            JPanel panel = new JPanel(new BorderLayout(0, 10));

            JLabel messageLabel = new JLabel(
                    "<html>" + message.replace("\n", "<br>") + "</html>"
            );

            panel.add(messageLabel, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);

            JOptionPane.showMessageDialog(
                    null,
                    panel,
                    "MusMeta - Unexpected Error",
                    JOptionPane.ERROR_MESSAGE
            );
        };

        try {

            if (SwingUtilities.isEventDispatchThread()) {
                showDialog.run();
            } else {
                SwingUtilities.invokeAndWait(showDialog);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            CRASH_DIALOG_CLOSED.countDown();
            System.exit(1);
        }
    }

    /**
     * Immediately prevents further interaction with the application while the
     * fatal-error dialog is prepared and displayed.
     */
    private static void freezeApplicationWindows() {
        Runnable freeze = () -> {
            for (Window window : Window.getWindows()) {
                if (window.isDisplayable()) {
                    window.setEnabled(false);
                }
            }
        };

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                freeze.run();
            } else {
                SwingUtilities.invokeAndWait(freeze);
            }
        } catch (Exception exception) {
            System.err.println("Failed to freeze application windows.");
            exception.printStackTrace();
        }
    }

    /** Subsequent fatal errors wait for the first dialog to close and exit. */
    private static void waitForTermination() {
        try {
            CRASH_DIALOG_CLOSED.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
