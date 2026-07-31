package com.freddy.verticaltabs;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(VerticalTabsConfig.GROUP)
public interface VerticalTabsConfig extends Config
{
    String GROUP = "verticaltabsreplacement";

    @ConfigSection(
        name = "Layout",
        description = "Size, arrangement, moving, and snapping",
        position = 0,
        closedByDefault = true
    )
    String LAYOUT_SECTION = "layout";

    @ConfigSection(
        name = "Interface",
        description = "Resizable Modern side-panel behavior",
        position = 1,
        closedByDefault = true
    )
    String INTERFACE_SECTION = "interface";

    @ConfigSection(
        name = "Appearance",
        description = "Opacity, labels, and colors",
        position = 2,
        closedByDefault = true
    )
    String APPEARANCE_SECTION = "appearance";

    @ConfigSection(
        name = "Buttons",
        description = "Choose which game tabs appear",
        position = 3,
        closedByDefault = true
    )
    String BUTTONS_SECTION = "buttons";

    @ConfigSection(
        name = "Advanced",
        description = "Custom order and original tabs",
        position = 4,
        closedByDefault = true
    )
    String ADVANCED_SECTION = "advanced";

    @ConfigItem(
        keyName = "showCombat",
        name = "Combat",
        description = "Show Combat Options",
        section = BUTTONS_SECTION,
        position = 0
    )
    default boolean showCombat()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showSkills",
        name = "Skills",
        description = "Show Skills",
        section = BUTTONS_SECTION,
        position = 1
    )
    default boolean showSkills()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showQuests",
        name = "Quests",
        description = "Show the Quest List",
        section = BUTTONS_SECTION,
        position = 2
    )
    default boolean showQuests()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showInventory",
        name = "Inventory",
        description = "Show Inventory",
        section = BUTTONS_SECTION,
        position = 3
    )
    default boolean showInventory()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showEquipment",
        name = "Equipment",
        description = "Show Worn Equipment",
        section = BUTTONS_SECTION,
        position = 4
    )
    default boolean showEquipment()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showPrayer",
        name = "Prayer",
        description = "Show Prayer",
        section = BUTTONS_SECTION,
        position = 5
    )
    default boolean showPrayer()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showMagic",
        name = "Magic",
        description = "Show Magic",
        section = BUTTONS_SECTION,
        position = 6
    )
    default boolean showMagic()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showFriendsChat",
        name = "Friends Chat",
        description = "Show Friends Chat",
        section = BUTTONS_SECTION,
        position = 7
    )
    default boolean showFriendsChat()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showIgnore",
        name = "Ignore",
        description = "Show the Ignore List",
        section = BUTTONS_SECTION,
        position = 8
    )
    default boolean showIgnore()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showFriends",
        name = "Friends",
        description = "Show the Friends List",
        section = BUTTONS_SECTION,
        position = 9
    )
    default boolean showFriends()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showLogout",
        name = "Logout",
        description = "Show Logout",
        section = BUTTONS_SECTION,
        position = 10
    )
    default boolean showLogout()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showOptions",
        name = "Options",
        description = "Show Options",
        section = BUTTONS_SECTION,
        position = 11
    )
    default boolean showOptions()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showEmotes",
        name = "Emotes",
        description = "Show Emotes",
        section = BUTTONS_SECTION,
        position = 12
    )
    default boolean showEmotes()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showMusic",
        name = "Music",
        description = "Show the Music Player",
        section = BUTTONS_SECTION,
        position = 13
    )
    default boolean showMusic()
    {
        return true;
    }

    @ConfigItem(
        keyName = "moveSeparately",
        name = "Move Separately",
        description = "Alt-drag each enabled button on its own",
        section = LAYOUT_SECTION,
        position = 0
    )
    default boolean moveSeparately()
    {
        return false;
    }

    @Range(min = 1, max = 14)
    @ConfigItem(
        keyName = "buttonsAcross",
        name = "Buttons Per Row",
        description = "Used when Move Separately is off; 1 makes a vertical dock",
        section = LAYOUT_SECTION,
        position = 1
    )
    default int buttonsPerRow()
    {
        return 1;
    }

    @Units(Units.PERCENT)
    @Range(min = 25, max = 200)
    @ConfigItem(
        keyName = "buttonScale",
        name = "Button Size",
        description = "Type 25-200; arrows change by 1; subpixel rendering smooths nearby sizes",
        section = LAYOUT_SECTION,
        position = 2
    )
    default int buttonScale()
    {
        return 100;
    }

    @Range(min = 0, max = 20)
    @ConfigItem(
        keyName = "gap",
        name = "Button Space",
        description = "Exact screen pixels between dock buttons and snapped loose buttons",
        section = LAYOUT_SECTION,
        position = 3
    )
    default int gap()
    {
        return 2;
    }

    @ConfigItem(
        keyName = "snapLooseButtons",
        name = "Snap Buttons",
        description = "Drop one loose button onto another to connect them",
        section = LAYOUT_SECTION,
        position = 4
    )
    default boolean snapLooseButtons()
    {
        return true;
    }

    @ConfigItem(
        keyName = "keepSidePanelOpen",
        name = "Lock Panel Open",
        description = "Prevent the active custom tab from closing and reopen the panel if the game collapses it",
        section = INTERFACE_SECTION,
        position = 0
    )
    default boolean keepSidePanelOpen()
    {
        return false;
    }

    @ConfigItem(
        keyName = "restoreLastSidePanel",
        name = "Restore Last Panel",
        description = "When Lock Panel Open is enabled, restore the last panel after login, hopping, or layout rebuilds",
        section = INTERFACE_SECTION,
        position = 1
    )
    default boolean restoreLastSidePanel()
    {
        return true;
    }

    @ConfigItem(
        keyName = "lastSidePanelTab",
        name = "",
        description = "",
        hidden = true
    )
    default int lastSidePanelTab()
    {
        return 3;
    }

    @Units(Units.PERCENT)
    @Range(min = 0, max = 100)
    @ConfigItem(
        keyName = "idleOpacity",
        name = "Idle Opacity",
        description = "Visibility of inactive buttons",
        section = APPEARANCE_SECTION,
        position = 0
    )
    default int idleOpacity()
    {
        return 45;
    }

    @Units(Units.PERCENT)
    @Range(min = 0, max = 100)
    @ConfigItem(
        keyName = "hoverOpacity",
        name = "Hover Opacity",
        description = "Visibility while hovering a button",
        section = APPEARANCE_SECTION,
        position = 1
    )
    default int hoverOpacity()
    {
        return 100;
    }

    @Units(Units.PERCENT)
    @Range(min = 0, max = 100)
    @ConfigItem(
        keyName = "selectedOpacity",
        name = "Active Opacity",
        description = "Visibility of the button for the open panel",
        section = APPEARANCE_SECTION,
        position = 2
    )
    default int selectedOpacity()
    {
        return 100;
    }

    @Units(Units.PERCENT)
    @Range(min = 0, max = 100)
    @ConfigItem(
        keyName = "frameOpacity",
        name = "Dock Opacity",
        description = "Visibility of the grouped dock background",
        section = APPEARANCE_SECTION,
        position = 3
    )
    default int frameOpacity()
    {
        return 75;
    }

    @ConfigItem(
        keyName = "showTooltip",
        name = "Show Names",
        description = "Show a tab name while hovering",
        section = APPEARANCE_SECTION,
        position = 4
    )
    default boolean showTooltip()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        keyName = "buttonColor",
        name = "Normal Color",
        description = "Color of inactive buttons",
        section = APPEARANCE_SECTION,
        position = 5
    )
    default Color buttonColor()
    {
        return new Color(49, 38, 26, 238);
    }

    @Alpha
    @ConfigItem(
        keyName = "hoverColor",
        name = "Hover Color",
        description = "Color while hovering a button",
        section = APPEARANCE_SECTION,
        position = 6
    )
    default Color hoverColor()
    {
        return new Color(82, 62, 36, 246);
    }

    @Alpha
    @ConfigItem(
        keyName = "selectedColor",
        name = "Active Color",
        description = "Color of the button for the open panel",
        section = APPEARANCE_SECTION,
        position = 7
    )
    default Color selectedColor()
    {
        return new Color(112, 82, 35, 250);
    }

    @Alpha
    @ConfigItem(
        keyName = "borderColor",
        name = "Border Color",
        description = "Color of button and dock borders",
        section = APPEARANCE_SECTION,
        position = 8
    )
    default Color borderColor()
    {
        return new Color(151, 113, 49, 248);
    }

    @ConfigItem(
        keyName = "tabOrder",
        name = "Button Order",
        description = "Type names separated by commas; unlisted buttons follow",
        section = ADVANCED_SECTION,
        position = 0
    )
    default String tabOrder()
    {
        return "combat, skills, quests, inventory, equipment, prayer, magic, friends chat, ignore, friends, logout, options, emotes, music";
    }

    @ConfigItem(
        keyName = "hideVanillaRail",
        name = "Hide Vanilla Tabs",
        description = "Hide RuneScape's original horizontal game tabs",
        section = ADVANCED_SECTION,
        position = 1
    )
    default boolean hideVanillaRail()
    {
        return true;
    }
}
