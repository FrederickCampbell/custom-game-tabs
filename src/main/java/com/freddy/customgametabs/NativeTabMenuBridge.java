package com.freddy.customgametabs;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;

/**
 * Projects the real Resizable Modern tab widget's menu semantics onto the
 * freely-positioned custom button.
 *
 * The native STONE remains the source of truth for action labels, operation
 * numbers, widget behavior, and component identity. Replaying MenuEntryAdded
 * while the entry still carries CC_OP identity lets RuneLite's MenuManager
 * attach any WidgetMenuOption registered for that STONE (for example the
 * Spellbook plugin's Magic-tab options) without depending on that plugin.
 */
@Singleton
final class NativeTabMenuBridge
{
    private final Client client;
    private final EventBus eventBus;

    @Inject
    NativeTabMenuBridge(
        Client client,
        EventBus eventBus
    )
    {
        this.client = client;
        this.eventBus = eventBus;
    }

    void prepareMenu(int tabIndex, boolean ownsSurface)
    {
        if (
            client.getGameState() != GameState.LOGGED_IN
                || client.isMenuOpen()
                || !ownsSurface
        )
        {
            return;
        }

        final Menu menu = client.getMenu();

        /* A real UI control occludes scene/NPC/object actions underneath it. */
        keepOnlyCancel(menu);

        if (!LayoutSpec.isValidTab(tabIndex))
        {
            ensureCancel(menu);
            return;
        }

        final LayoutSpec layout = LayoutSpec.forRoot(
            client.getTopLevelInterfaceId()
        );
        if (layout == null)
        {
            ensureCancel(menu);
            return;
        }

        final Widget stone = client.getWidget(layout.getStoneId(tabIndex));
        if (stone == null)
        {
            ensureCancel(menu);
            return;
        }

        buildNativeOperations(menu, stone);
        // Third-party widget menus may be inserted above the actual native tab action.
        // Keep them in the right-click menu, but leave the native op as the default.
        prioritizeNativePrimaryAction(menu, stone.getId());
        ensureCancel(menu);
    }

    private void buildNativeOperations(
        Menu menu,
        Widget stone
    )
    {
        final String[] actions = stone.getActions();
        if (actions == null)
        {
            return;
        }

        /*
         * The menu is displayed in reverse insertion order, so recreate widget
         * ops from the highest slot down to op 1.
         */
        for (int actionIndex = actions.length - 1;
             actionIndex >= 0;
             actionIndex--)
        {
            final String action = actions[actionIndex];
            if (action == null || action.isBlank())
            {
                continue;
            }

            final int op = actionIndex + 1;
            final MenuEntry entry = menu.createMenuEntry(-1)
                .setOption(action)
                .setTarget("")
                .setType(nativeActionType(op))
                .setIdentifier(op)
                /*
                 * These are static top-level widgets. Native CC_OP menu entries
                 * use child/index -1 and the full component id in param1.
                 */
                .setParam0(-1)
                .setParam1(stone.getId());

            /*
             * Creating a CC_OP-shaped entry is not enough for RuneLite-managed
             * widget options: MenuManager attaches those from MenuEntryAdded.
             * Dispatch the event with the real native component id and leave the
             * base entry as CC_OP afterward. That lets RuneScape execute the
             * complete native operation when the user selects it.
             */
            eventBus.post(new MenuEntryAdded(entry));
        }
    }

    /**
     * RuneLite-managed widget options are inserted at the top when MenuEntryAdded
     * is replayed. That is appropriate for a right-click submenu, but would make
     * a plugin's secondary option the default/Mouse Highlight label for this tab.
     *
     * Reorder the existing entries without creating new operations, deleting
     * plugin actions, or changing callbacks or CC_OP identity.
     */
    static void prioritizeNativePrimaryAction(Menu menu, int widgetId)
    {
        final MenuEntry[] entries = menu.getMenuEntries();
        if (entries == null || entries.length < 2)
        {
            return;
        }

        for (int i = 0; i < entries.length - 1; i++)
        {
            final MenuEntry entry = entries[i];
            if (entry.getType() != MenuAction.CC_OP
                || entry.getIdentifier() != 1
                || entry.getParam0() != -1
                || entry.getParam1() != widgetId)
            {
                continue;
            }

            final MenuEntry[] ordered = entries.clone();
            System.arraycopy(ordered, i + 1, ordered, i, ordered.length - i - 1);
            ordered[ordered.length - 1] = entry;
            menu.setMenuEntries(ordered);
            return;
        }
    }

    private static MenuAction nativeActionType(int op)
    {
        return op <= 5
            ? MenuAction.CC_OP
            : MenuAction.CC_OP_LOW_PRIORITY;
    }

    private static void keepOnlyCancel(Menu menu)
    {
        final MenuEntry[] entries = menu.getMenuEntries();
        if (entries == null)
        {
            return;
        }

        for (MenuEntry entry : entries)
        {
            if (entry.getType() != MenuAction.CANCEL)
            {
                menu.removeMenuEntry(entry);
            }
        }
    }

    private static void ensureCancel(Menu menu)
    {
        final MenuEntry[] entries = menu.getMenuEntries();
        if (entries != null)
        {
            for (MenuEntry entry : entries)
            {
                if (entry.getType() == MenuAction.CANCEL)
                {
                    return;
                }
            }
        }

        menu.createMenuEntry(0)
            .setOption("Cancel")
            .setTarget("")
            .setType(MenuAction.CANCEL)
            .setIdentifier(0)
            .setParam0(0)
            .setParam1(0);
    }
}
