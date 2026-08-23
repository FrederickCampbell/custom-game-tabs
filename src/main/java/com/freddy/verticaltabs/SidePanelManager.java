package com.freddy.verticaltabs;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;

/**
 * Owns the Resizable Modern side-panel open/restore behavior.
 *
 * Tab activation reuses RuneScape's native top-level tab OnOp listener. The
 * plugin does not synthesize input or directly dispatch menu actions.
 */
@Singleton
final class SidePanelManager
{
    private static final int REOPEN_COOLDOWN_CLIENT_TICKS = 3;

    private final Client client;
    private final ConfigManager configManager;
    private final VerticalTabsConfig config;

    private int rememberedTab = LayoutSpec.INVENTORY_TAB;
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
            : LayoutSpec.INVENTORY_TAB;
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

        if (layout == null || !LayoutSpec.isValidTab(tabIndex))
        {
            return;
        }

        if (!validRestorablePanelTab(tabIndex))
        {
            invokeNativeTabListener(layout, tabIndex);
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

        /*
         * If the client still remembers this tab while the panel itself is
         * collapsed, reset the selection before running the same native listener
         * a physical click on the original tab would run.
         */
        if (selectedTab == tabIndex && !panelOpen)
        {
            client.setVarcIntValue(VarClientID.TOPLEVEL_PANEL, -1);
        }

        panelOpenedThisSession = true;
        rememberTab(tabIndex);
        reopenCooldown = REOPEN_COOLDOWN_CLIENT_TICKS;
        invokeNativeTabListener(layout, tabIndex);
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
        invokeNativeTabListener(layout, rememberedTab);
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

    private void invokeNativeTabListener(
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

        final Object[] onOpListener = source.getOnOpListener();
        if (onOpListener == null || onOpListener.length == 0)
        {
            return;
        }

        /*
         * This is the original RuneScape top-level tab listener. A real user
         * click on the custom overlay maps 1:1 to the corresponding native tab
         * operation without direct menu dispatch or synthetic mouse/keyboard input.
         */
        client.createScriptEventBuilder(onOpListener)
            .setSource(source)
            .setOp(1)
            .build()
            .run();
    }

    private static boolean validRestorablePanelTab(int tabIndex)
    {
        return LayoutSpec.isValidTab(tabIndex)
            && tabIndex != LayoutSpec.LOGOUT_TAB;
    }
}
