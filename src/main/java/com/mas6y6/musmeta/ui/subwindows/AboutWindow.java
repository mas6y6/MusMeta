package com.mas6y6.musmeta.ui.subwindows;

import com.mas6y6.musmeta.utils.ScrollUtil;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class AboutWindow extends JDialog {

    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().build();

    public AboutWindow(JFrame parent) {
        super(parent, "About", true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Banner
        JLabel banner = new JLabel();

        ImageIcon originalIcon = new ImageIcon(
                getClass().getResource("/banner.png")
        );

        int windowWidth = 450;
        int bannerHeight = (int) (
                (double) originalIcon.getIconHeight()
                        / originalIcon.getIconWidth()
                        * windowWidth
        );

        Image scaledImage = originalIcon.getImage().getScaledInstance(
                windowWidth,
                bannerHeight,
                Image.SCALE_SMOOTH
        );

        banner.setIcon(new ImageIcon(scaledImage));
        banner.setHorizontalAlignment(SwingConstants.CENTER);

        content.add(banner, BorderLayout.NORTH);

        // Markdown
        JEditorPane markdown = new JEditorPane();
        markdown.setContentType("text/html");
        markdown.setEditable(false);
        markdown.setOpaque(false);
        markdown.setText(loadMarkdown("/about.md"));

        JScrollPane scrollPane = new JScrollPane(markdown);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        javax.swing.SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));

        content.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dispose());

        buttons.add(okButton);

        content.add(buttons, BorderLayout.SOUTH);

        setContentPane(content);

        setSize(500, 500);
        setLocationRelativeTo(parent);

        getRootPane().setDefaultButton(okButton);

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private String loadMarkdown(String resource) {
        try (InputStream input = getClass().getResourceAsStream(resource)) {

            if (input == null) {
                return "<html><body><b>About information unavailable.</b></body></html>";
            }

            String markdown;

            try (InputStreamReader reader =
                         new InputStreamReader(input, StandardCharsets.UTF_8)) {

                markdown = new BufferedReader(reader)
                        .lines()
                        .collect(Collectors.joining("\n"));
            }

            Node document = MARKDOWN_PARSER.parse(markdown);

            return HTML_RENDERER.render(document);

        } catch (IOException e) {
            return "<html><body><b>Failed to load about information.</b></body></html>";
        }
    }
}