package com.freddy.customgametabs;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;

/**
 * Thin adapter around RuneScape's own Resizable Modern tab state.
 *
 * Custom Game Tabs deliberately does not own side-panel lifetime anymore.
 * RuneScape decides when the panel opens, closes, is replaced by Bank/Shop/
 * storage interfaces, and what happens when those interfaces go away. We only
 * observe TOPLEVEL_PANEL for highlighting and reuse each native stone's OnOp
 * listener for direct left-click activation.
 */
@Singleton
final class NativeTabController
{
    private final Client client;

    /* Read by overlay rendering; writes happen on the client thread. */
    private volatile int displayedTab = -1;
    private volatile String[] tabNames = fallbackTabNames();

    @Inject
    NativeTabController(Client client)
    {
        this.client = client;
    }

    void startUp()
    {
        refreshDisplayedTab();
    }

    void shutDown()
    {
        displayedTab = -1;
        tabNames = fallbackTabNames();
    }

    void onGameStateChanged(GameState gameState)
    {
        if (gameState == GameState.LOGGED_IN)
        {
            refreshDisplayedTab();
        }
        else
        {
            displayedTab = -1;
            tabNames = fallbackTabNames();
        }
    }

    void onTopLevelRebuilt()
    {
        refreshDisplayedTab();
    }

    void onTopLevelPanelChanged()
    {
        refreshDisplayedTab();
    }

    void onPostClientTick()
    {
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            refreshDisplayedTab();
        }
    }

    boolean isTabActive(int tabIndex)
    {
        return displayedTab == tabIndex;
    }

    String getTabName(int tabIndex)
    {
        if (!LayoutSpec.isValidTab(tabIndex))
        {
            return "";
        }

        final String[] names = tabNames;
        final String name = names[tabIndex];

        return name == null || name.isBlank()
            ? LayoutSpec.TABS[tabIndex].getName()
            : name;
    }

    void activateTab(int tabIndex)
    {
        activateTabOperation(tabIndex, 1);
    }

    void activateTabOperation(int tabIndex, int op)
    {
        final LayoutSpec layout = LayoutSpec.forRoot(
            client.getTopLevelInterfaceId()
        );

        if (
            layout == null
                || !LayoutSpec.isValidTab(tabIndex)
                || op < 1
        )
        {
            return;
        }

        /*
         * Left-click is an overlay click, not a RuneScape widget click, so reuse
         * the backing STONE's real listener. Right-click menu operations do not
         * come through here; NativeTabMenuBridge leaves them as genuine CC_OP
         * entries so RuneScape executes their full native widget-action path.
         */
        invokeNativeTabListener(layout, tabIndex, op);
    }

    private void refreshDisplayedTab()
    {
        final LayoutSpec layout = LayoutSpec.forRoot(
            client.getTopLevelInterfaceId()
        );

        if (layout == null)
        {
            displayedTab = -1;
            tabNames = fallbackTabNames();
            return;
        }

        refreshTabNames(layout);

        final int selectedTab = client.getVarcIntValue(
            VarClientID.TOPLEVEL_PANEL
        );

        if (!LayoutSpec.isValidTab(selectedTab))
        {
            displayedTab = -1;
            return;
        }

        /*
         * Logout is its own top-level tab. Every other normal tab is active
         * only while RuneScape's normal side panel is actually visible.
         */
        displayedTab = selectedTab == LayoutSpec.LOGOUT_TAB
            ? selectedTab
            : layout.isSidePanelOpen(client)
                ? selectedTab
                : -1;
    }

    private void refreshTabNames(LayoutSpec layout)
    {
        final String[] refreshed = fallbackTabNames();

        for (int tabIndex = 0; tabIndex < refreshed.length; tabIndex++)
        {
            final Widget stone = client.getWidget(
                layout.getStoneId(tabIndex)
            );

            if (stone == null)
            {
                continue;
            }

            final String[] actions = stone.getActions();
            if (
                actions != null
                    && actions.length > 0
                    && actions[0] != null
                    && !actions[0].isBlank()
            )
            {
                refreshed[tabIndex] = actions[0];
            }
        }

        tabNames = refreshed;
    }

    private boolean invokeNativeTabListener(
        LayoutSpec layout,
        int tabIndex,
        int op
    )
    {
        final Widget source = client.getWidget(
            layout.getStoneId(tabIndex)
        );
        if (source == null)
        {
            return false;
        }

        final Object[] onOpListener = source.getOnOpListener();
        if (onOpListener == null || onOpListener.length == 0)
        {
            return false;
        }

        client.createScriptEventBuilder(onOpListener)
            .setSource(source)
            .setOp(op)
            .build()
            .run();
        return true;
    }

    private static String[] fallbackTabNames()
    {
        final String[] names = new String[LayoutSpec.TABS.length];

        for (int index = 0; index < names.length; index++)
        {
            names[index] = LayoutSpec.TABS[index].getName();
        }

        return names;
    }
}
