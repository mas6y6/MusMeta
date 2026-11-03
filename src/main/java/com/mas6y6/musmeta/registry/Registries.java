package com.mas6y6.musmeta.registry;

import com.mas6y6.musmeta.registry.items.AudioConversionFormatRegistryItem;
import com.mas6y6.musmeta.registry.items.AudioExtensionRegistryItem;
import com.mas6y6.musmeta.registry.items.DialogRegistryItem;
import com.mas6y6.musmeta.registry.items.MainTabRegistryItem;
import com.mas6y6.musmeta.registry.items.SettingTabRegistryItem;

import javax.swing.*;

public final class Registries {
    private static boolean frozen;

    public static final Registry<String, SettingTabRegistryItem> SETTING_TABS = new Registry<>("SETTING_TABS");
    public static final Registry<String, JMenu> MENU_ITEMS = new Registry<>("MENU_ITEMS");
    public static final Registry<String, JMenuItem> FILE_MENU_ITEMS = new Registry<>("FILE_MENU_ITEMS");
    public static final Registry<String, JMenuItem> EDIT_MENU_ITEMS = new Registry<>("EDIT_MENU_ITEMS");
    public static final Registry<String, JMenuItem> LIBRARY_MENU_ITEMS = new Registry<>("LIBRARY_MENU_ITEMS");
    public static final Registry<String, JMenuItem> VIEW_MENU_ITEMS = new Registry<>("VIEW_MENU_ITEMS");
    public static final Registry<String, JComponent> TOOLBAR_ITEMS = new Registry<>("TOOLBAR_ITEMS");
    public static final Registry<String, MainTabRegistryItem> MAIN_TABS = new Registry<>("MAIN_TABS");
    public static final Registry<String, JMenuItem> CONTEXT_MENU_ITEMS = new Registry<>("CONTEXT_MENU_ITEMS");
    public static final Registry<String, JComponent> STATUS_BAR_ITEMS = new Registry<>("STATUS_BAR_ITEMS");
    public static final Registry<String, DialogRegistryItem> DIALOGS = new Registry<>("DIALOGS");
    public static final Registry<String, AudioExtensionRegistryItem> AUDIO_EXTENSIONS = new Registry<>("AUDIO_EXTENSIONS");
    public static final Registry<String, AudioConversionFormatRegistryItem> AUDIO_CONVERSION_FORMATS = new Registry<>("AUDIO_CONVERSION_FORMATS");

    // Convenience aliases for common extension points.
    public static final Registry<String, JComponent> TOOLBARS = TOOLBAR_ITEMS;
    public static final Registry<String, MainTabRegistryItem> TABS = MAIN_TABS;
    public static final Registry<String, JMenuItem> POPUP_MENU_ITEMS = CONTEXT_MENU_ITEMS;

    private Registries() {
    }

    public static void freezeAll() {
        if (frozen) return;
        frozen = true;
        for (Registry<?, ?> registry : Registry.all()) {
            registry.freeze();
        }
    }

    static boolean isFrozen() {
        return frozen;
    }
}