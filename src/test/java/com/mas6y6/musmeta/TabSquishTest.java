package com.mas6y6.musmeta;

import com.formdev.flatlaf.FlatLightLaf;
import com.mas6y6.musmeta.ui.prompts.PostInstallationPrompt;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TabSquishTest {
    @Test
    public void testTabsWidthConsistency() throws Exception {
        FlatLightLaf.setup();
        SwingUtilities.invokeAndWait(() -> {
            try {
                PostInstallationPrompt prompt = new PostInstallationPrompt();
                Field tabsField = PostInstallationPrompt.class.getDeclaredField("tabs");
                tabsField.setAccessible(true);
                JTabbedPane tabs = (JTabbedPane) tabsField.get(prompt);

                Method nextMethod = PostInstallationPrompt.class.getDeclaredMethod("nextPage");
                nextMethod.setAccessible(true);

                int initialWidth = tabs.getBoundsAt(0).width;

                for (int i = 0; i < 4; i++) {
                    for (int tabIdx = 0; tabIdx < 4; tabIdx++) {
                        assertEquals(initialWidth, tabs.getBoundsAt(tabIdx).width,
                                "Tab " + tabIdx + " width changed on page " + i);
                    }
                    if (i < 3) {
                        nextMethod.invoke(prompt);
                    }
                }
                prompt.dispose();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
