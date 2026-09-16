package com.freddy.customgametabs;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(CustomGameTabsConfig.GROUP)
public interface CustomGameTabsConfig extends Config
{
    /* Stable persisted namespace from v1; changing it would orphan existing profiles. */
    String GROUP = "verticaltabsreplacement";

    @ConfigSection(
        name = "Appearance",
        description = "Global size, spacing, opacity, tooltips, and colors",
        position = 0,
        closedByDefault = false
    )
    String APPEARANCE_SECTION = "appearance";

    @ConfigSection(
        name = "Behavior",
        description = "Global interaction behavior",
        position = 1,
        closedByDefault = false
    )
    String BEHAVIOR_SECTION = "behavior";

    /* Activity-layout controls are intentionally hidden from RuneLite's static
     * ConfigPanel. The live Tab Layouts sidebar owns these so loaded presets can
     * update the controls immediately without stale UI. */
    @ConfigItem(keyName = "layoutMode", name = "Layout Mode", description = "", hidden = true)
    default TabLayoutMode layoutMode()
    {
        return TabLayoutMode.DOCKED;
    }

    @Range(min = 1, max = 14)
    @ConfigItem(keyName = "buttonsAcross", name = "Buttons Per Row", description = "", hidden = true)
    default int buttonsPerRow()
    {
        return 1;
    }

    @ConfigItem(keyName = "showCombat", name = "Combat", description = "", hidden = true)
    default boolean showCombat() { return true; }

    @ConfigItem(keyName = "showSkills", name = "Skills", description = "", hidden = true)
    default boolean showSkills() { return true; }

    @ConfigItem(keyName = "showQuests", name = "Quests", description = "", hidden = true)
    default boolean showQuests() { return true; }

    @ConfigItem(keyName = "showInventory", name = "Inventory", description = "", hidden = true)
    default boolean showInventory() { return true; }

    @ConfigItem(keyName = "showEquipment", name = "Equipment", description = "", hidden = true)
    default boolean showEquipment() { return true; }

    @ConfigItem(keyName = "showPrayer", name = "Prayer", description = "", hidden = true)
    default boolean showPrayer() { return true; }

    @ConfigItem(keyName = "showMagic", name = "Magic", description = "", hidden = true)
    default boolean showMagic() { return true; }

    @ConfigItem(keyName = "showFriendsChat", name = "Friends Chat", description = "", hidden = true)
    default boolean showFriendsChat() { return true; }

    @ConfigItem(keyName = "showIgnore", name = "Ignore", description = "", hidden = true)
    default boolean showIgnore() { return true; }

    @ConfigItem(keyName = "showFriends", name = "Friends", description = "", hidden = true)
    default boolean showFriends() { return true; }

    @ConfigItem(keyName = "showLogout", name = "Logout", description = "", hidden = true)
    default boolean showLogout() { return true; }

    @ConfigItem(keyName = "showOptions", name = "Options", description = "", hidden = true)
    default boolean showOptions() { return true; }

    @ConfigItem(keyName = "showEmotes", name = "Emotes", description = "", hidden = true)
    default boolean showEmotes() { return true; }

    @ConfigItem(keyName = "showMusic", name = "Music", description = "", hidden = true)
    default boolean showMusic() { return true; }

    @ConfigItem(keyName = "tabOrder", name = "Button Order", description = "", hidden = true)
    default String tabOrder()
    {
        return "combat, skills, quests, inventory, equipment, prayer, magic, friends chat, ignore, friends, logout, options, emotes, music";
    }

    @ConfigItem(keyName = "drawerTabs", name = "Drawer Tabs", description = "", hidden = true)
    default String drawerTabs()
    {
        return "";
    }

    @ConfigItem(keyName = "drawerEnabled", name = "Drawer Enabled", description = "", hidden = true)
    default boolean drawerEnabled()
    {
        return true;
    }

    @ConfigItem(keyName = "drawerDirection", name = "Drawer Direction", description = "", hidden = true)
    default DrawerDirection drawerDirection()
    {
        return DrawerDirection.AUTO;
    }

    @ConfigItem(keyName = "drawerCloseAfterSelection", name = "Close Drawer After Selection", description = "", hidden = true)
    default boolean drawerCloseAfterSelection()
    {
        return true;
    }

    @ConfigItem(keyName = "stickTogether", name = "Stick Together", description = "", hidden = true)
    default boolean stickTogether()
    {
        return false;
    }

    @Units(Units.PERCENT)
    @Range(min = 25, max = 200)
    @ConfigItem(
        keyName = "buttonScale",
        name = "Button Size",
        description = "Type 25-200; arrows change by 1; subpixel rendering smooths nearby sizes",
        section = APPEARANCE_SECTION,
        position = 0
    )
    default int buttonScale()
    {
        return 100;
    }

    @Range(min = 0, max = 20)
    @ConfigItem(
        keyName = "gap",
        name = "Button Spacing",
        description = "Exact screen pixels between dock buttons, drawer buttons, and snapped freeform buttons",
        section = APPEARANCE_SECTION,
        position = 1
    )
    default int gap()
    {
        return 2;
    }

    @Units(Units.PERCENT)
    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "idleOpacity", name = "Idle Opacity", description = "Visibility of inactive buttons", section = APPEARANCE_SECTION, position = 2)
    default int idleOpacity() { return 45; }

    @Units(Units.PERCENT)
    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "hoverOpacity", name = "Hover Opacity", description = "Visibility while hovering a button", section = APPEARANCE_SECTION, position = 3)
    default int hoverOpacity() { return 100; }

    @Units(Units.PERCENT)
    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "selectedOpacity", name = "Active Opacity", description = "Visibility of the button for the open panel", section = APPEARANCE_SECTION, position = 4)
    default int selectedOpacity() { return 100; }

    @Units(Units.PERCENT)
    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "frameOpacity", name = "Dock Opacity", description = "Visibility of the grouped dock background", section = APPEARANCE_SECTION, position = 5)
    default int frameOpacity() { return 75; }

    @ConfigItem(
        keyName = "showTooltip",
        name = "Show Tab Names",
        description = "Show the hovered tab name using RuneLite's shared tooltip system when no other tooltip is active",
        section = APPEARANCE_SECTION,
        position = 6
    )
    default boolean showTooltip() { return false; }

    @Alpha
    @ConfigItem(keyName = "buttonColor", name = "Normal Color", description = "Color of inactive buttons", section = APPEARANCE_SECTION, position = 7)
    default Color buttonColor() { return new Color(49, 38, 26, 238); }

    @Alpha
    @ConfigItem(keyName = "hoverColor", name = "Hover Color", description = "Color while hovering a button", section = APPEARANCE_SECTION, position = 8)
    default Color hoverColor() { return new Color(82, 62, 36, 246); }

    @Alpha
    @ConfigItem(keyName = "selectedColor", name = "Active Color", description = "Color of the button for the open panel", section = APPEARANCE_SECTION, position = 9)
    default Color selectedColor() { return new Color(112, 82, 35, 250); }

    @Alpha
    @ConfigItem(keyName = "borderColor", name = "Border Color", description = "Color of button, drawer, and dock borders", section = APPEARANCE_SECTION, position = 10)
    default Color borderColor() { return new Color(151, 113, 49, 248); }

    @ConfigItem(
        keyName = "snapLooseButtons",
        name = "Snap Buttons",
        description = "In Freeform mode, dropping one loose button onto another places it directly beside that button",
        section = BEHAVIOR_SECTION,
        position = 0
    )
    default boolean snapLooseButtons()
    {
        return true;
    }
}
