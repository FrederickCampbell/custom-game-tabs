package com.freddy.verticaltabs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class TabLayout
{
    private static final String[] SHOW_KEYS = new String[]
    {
        "showCombat",
        "showSkills",
        "showQuests",
        "showInventory",
        "showEquipment",
        "showPrayer",
        "showMagic",
        "showFriendsChat",
        "showIgnore",
        "showFriends",
        "showLogout",
        "showOptions",
        "showEmotes",
        "showMusic"
    };

    private TabLayout()
    {
    }

    static List<Integer> visibleOrder(VerticalTabsConfig config)
    {
        final LinkedHashSet<Integer> order =
            parse(config.tabOrder());

        for (int index = 0; index < LayoutSpec.TABS.length; index++)
        {
            order.add(index);
        }

        final List<Integer> result = new ArrayList<>();
        for (int tabIndex : order)
        {
            if (isShown(config, tabIndex))
            {
                result.add(tabIndex);
            }
        }

        return result;
    }

    static String showKey(int tabIndex)
    {
        return tabIndex >= 0 && tabIndex < SHOW_KEYS.length
            ? SHOW_KEYS[tabIndex]
            : "";
    }

    static boolean isShown(
        VerticalTabsConfig config,
        int tabIndex
    )
    {
        switch (tabIndex)
        {
            case 0:
                return config.showCombat();
            case 1:
                return config.showSkills();
            case 2:
                return config.showQuests();
            case 3:
                return config.showInventory();
            case 4:
                return config.showEquipment();
            case 5:
                return config.showPrayer();
            case 6:
                return config.showMagic();
            case 7:
                return config.showFriendsChat();
            case 8:
                return config.showIgnore();
            case 9:
                return config.showFriends();
            case 10:
                return config.showLogout();
            case 11:
                return config.showOptions();
            case 12:
                return config.showEmotes();
            case 13:
                return config.showMusic();
            default:
                return false;
        }
    }

    static LinkedHashSet<Integer> parse(String text)
    {
        final LinkedHashSet<Integer> result =
            new LinkedHashSet<>();

        if (text == null || text.isBlank())
        {
            return result;
        }

        for (String raw : text.split("[,;\\n]+"))
        {
            final int resolved = resolve(raw);
            if (resolved >= 0)
            {
                result.add(resolved);
            }
        }

        return result;
    }

    private static int resolve(String raw)
    {
        final String token = normalize(raw);
        if (token.isEmpty())
        {
            return -1;
        }

        switch (token)
        {
            case "0":
            case "combat":
            case "combatoptions":
            case "attack":
                return 0;
            case "1":
            case "skills":
            case "stats":
                return 1;
            case "2":
            case "quest":
            case "quests":
            case "questlist":
                return 2;
            case "3":
            case "inventory":
            case "inv":
            case "bag":
                return 3;
            case "4":
            case "equipment":
            case "wornequipment":
            case "gear":
                return 4;
            case "5":
            case "prayer":
            case "prayers":
                return 5;
            case "6":
            case "magic":
            case "spellbook":
            case "spells":
                return 6;
            case "7":
            case "friendschat":
            case "friendchat":
            case "fc":
            case "clanchat":
                return 7;
            case "8":
            case "ignore":
            case "ignorelist":
                return 8;
            case "9":
            case "friend":
            case "friends":
            case "friendlist":
                return 9;
            case "10":
            case "logout":
            case "exit":
                return 10;
            case "11":
            case "option":
            case "options":
            case "setting":
            case "settings":
                return 11;
            case "12":
            case "emote":
            case "emotes":
                return 12;
            case "13":
            case "music":
            case "musicplayer":
                return 13;
            default:
                return -1;
        }
    }

    private static String normalize(String value)
    {
        return value
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");
    }
}
