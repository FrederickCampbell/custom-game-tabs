package com.freddy.verticaltabs;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;

/**
 * Owns the Resizable Modern side-panel open/restore behavior.
 *
 * Panel scaling is intentionally not implemented here. Tab activation keeps
 * RuneScape's native widget geometry and remains immediate and reliable.
 */
@Singleton
final class SidePanelManager
{
    private static final int INVENTORY_TAB = 3;
    private static final int REOPEN_COOLDOWN_CLIENT_TICKS = 3;

    private final Client client;
    private final ConfigManager configManager;
    private final VerticalTabsConfig config;

    private int rememberedTab = INVENTORY_TAB;
    private int reopenCooldown;
    private boolean panelOpenedThisSession;

    @Inject
    SidePanelManager(
        Client client,
        ConfigManager configManager,
        VerticalTabsConfig config
    )
    {
        this.client = client;
        this.configManager = configManager;
        this.config = config;
    }

    void startUp()
    {
        rememberedTab = validRestorablePanelTab(config.lastSidePanelTab())
            ? config.lastSidePanelTab()
            : INVENTORY_TAB;
        reopenCooldown = 0;
        panelOpenedThisSession = false;
    }

    void shutDown()
    {
        panelOpenedThisSession = false;
        reopenCooldown = 0;
    }

    void onGameStateChanged(GameState gameState)
    {
        if (gameState != GameState.LOGGED_IN)
        {
            panelOpenedThisSession = false;
            reopenCooldown = 0;
        }
    }

    void onConfigChanged(String key)
    {
        if ("keepSidePanelOpen".equals(key))
        {
            reopenCooldown = 0;
            if (!config.keepSidePanelOpen())
            {
                panelOpenedThisSession = false;
            }
        }
    }

    void onPostClientTick()
    {
        if (reopenCooldown > 0)
        {
            reopenCooldown--;
        }

        if (client.getGameState() == GameState.LOGGED_IN)
        {
            maintainLockedPanel();
        }
    }

    void activateTab(int tabIndex)
    {
        final LayoutSpec layout = LayoutSpec.forRoot(
            client.getTopLevelInterfaceId()
        );

        if (layout == null || !validTab(tabIndex))
        {
            return;
        }

        if (!validRestorablePanelTab(tabIndex))
        {
            invokeNativeTabAction(layout, tabIndex);
            return;
        }

        final int selectedTab = client.getVarcIntValue(
            VarClientID.TOPLEVEL_PANEL
        );
        final boolean panelOpen = layout.isSidePanelOpen(client);

        if (
            config.keepSidePanelOpen()
                && panelOpen
                && selectedTab == tabIndex
        )
        {
            panelOpenedThisSession = true;
            rememberTab(tabIndex);
            return;
        }

        if (selectedTab == tabIndex && !panelOpen)
        {
            client.setVarcIntValue(
                VarClientID.TOPLEVEL_PANEL,
                -1
            );
        }

        panelOpenedThisSession = true;
        rememberTab(tabIndex);
        reopenCooldown = REOPEN_COOLDOWN_CLIENT_TICKS;
        invokeNativeTabAction(layout, tabIndex);
    }

    private void maintainLockedPanel()
    {
        final LayoutSpec layout = LayoutSpec.forRoot(
            client.getTopLevelInterfaceId()
        );
        if (layout == null)
        {
            return;
        }

        final int selectedTab = client.getVarcIntValue(
            VarClientID.TOPLEVEL_PANEL
        );
        final boolean panelOpen = layout.isSidePanelOpen(client);

        if (panelOpen && validRestorablePanelTab(selectedTab))
        {
            panelOpenedThisSession = true;
            rememberTab(selectedTab);
            return;
        }

        if (
            !config.keepSidePanelOpen()
                || reopenCooldown > 0
                || (!panelOpenedThisSession
                    && !config.restoreLastSidePanel())
        )
        {
            return;
        }

        if (selectedTab == rememberedTab)
        {
            client.setVarcIntValue(VarClientID.TOPLEVEL_PANEL, -1);
        }

        reopenCooldown = REOPEN_COOLDOWN_CLIENT_TICKS;
        invokeNativeTabAction(layout, rememberedTab);
    }

    private void rememberTab(int tabIndex)
    {
        if (!validRestorablePanelTab(tabIndex) || rememberedTab == tabIndex)
        {
            return;
        }

        rememberedTab = tabIndex;
        configManager.setConfiguration(
            VerticalTabsConfig.GROUP,
            "lastSidePanelTab",
            tabIndex
        );
    }

    private void invokeNativeTabAction(
        LayoutSpec layout,
        int tabIndex
    )
    {
        final Widget source = client.getWidget(
            layout.getStoneId(tabIndex)
        );
        if (source == null)
        {
            return;
        }

        final String[] actions = source.getActions();
        final String action = actions != null
            && actions.length > 0
            && actions[0] != null
            && !actions[0].isBlank()
            ? actions[0]
            : "Select";

        client.menuAction(
            -1,
            source.getId(),
            MenuAction.CC_OP,
            1,
            -1,
            action,
            ""
        );
    }

    private static boolean validTab(int tabIndex)
    {
        return tabIndex >= 0
            && tabIndex < LayoutSpec.TABS.length;
    }

    private static boolean validRestorablePanelTab(int tabIndex)
    {
        return validTab(tabIndex) && tabIndex != 10;
    }
}
