package com.freddy.verticaltabs;

import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WidgetNode;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;

/**
 * Owns Resizable Modern side-panel state for Custom Game Tabs.
 *
 * Important boundary: this class does not own keyboard mappings. RuneScape,
 * RuneLite Key Remapping, and other plugins may legitimately cause a normal
 * game-tab change. We observe TOPLEVEL_PANEL and the real panel visibility and
 * reflect that change; we never force the previous tab back over a successful
 * external tab switch.
 *
 * Temporary interfaces (Bank, Shop, GE, Trade, storage, etc.) are a separate
 * state. They can temporarily own the side area while TOPLEVEL_PANEL changes or
 * remains stale. Those interfaces suppress CUSTOM non-Logout clicks so a click
 * cannot be queued behind them, but they do not intercept keyboard/plugin input.
 */
@Singleton
final class SidePanelManager
{
    private static final int RESTORE_DELAY_CLIENT_TICKS = 3;

    private final Client client;
    private final ConfigManager configManager;
    private final VerticalTabsConfig config;
    private final Set<Integer> activeBlockingGroups = new HashSet<>();

    private int rememberedTab = LayoutSpec.INVENTORY_TAB;
    private int restoreDelay;
    private boolean restorePending;
    private boolean restoreAfterLogout;
    private boolean restoreAfterBlocking;
    private boolean blockingLastTick;

    /* Read by overlay rendering; writes happen on the client thread. */
    private volatile boolean blockingInterfaceActive;
    private volatile boolean inventoryOverrideActive;
    private volatile int displayedTab = -1;

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
        resetTransientState();

        if (client.getGameState() == GameState.LOGGED_IN)
        {
            reconcileBlockingInterfaces();
            requestLifecycleRestore();
        }
    }

    void shutDown()
    {
        resetTransientState();
    }

    void onGameStateChanged(GameState gameState)
    {
        displayedTab = -1;

        if (gameState == GameState.LOGGED_IN)
        {
            reconcileBlockingInterfaces();
            requestLifecycleRestore();
            return;
        }

        activeBlockingGroups.clear();
        updateBlockingFlags();
        restorePending = false;
        restoreDelay = 0;
        restoreAfterLogout = false;
        restoreAfterBlocking = false;
        blockingLastTick = false;
    }

    void onWidgetLoaded(int groupId)
    {
        if (LayoutSpec.isBlockingInterface(groupId))
        {
            activeBlockingGroups.add(groupId);
            updateBlockingFlags();
        }
    }

    void onWidgetClosed(int groupId, boolean unload)
    {
        /* unload=false means the interface will be immediately reloaded. */
        if (unload && activeBlockingGroups.remove(groupId))
        {
            updateBlockingFlags();
        }
    }

    void onTopLevelRebuilt()
    {
        reconcileBlockingInterfaces();
        requestLifecycleRestore();
    }

    void onConfigChanged(String key)
    {
        if (
            "keepSidePanelOpen".equals(key)
                || "restoreLastSidePanel".equals(key)
        )
        {
            if (
                config.keepSidePanelOpen()
                    && config.restoreLastSidePanel()
            )
            {
                requestLifecycleRestore();
            }
            else
            {
                restorePending = false;
                restoreDelay = 0;
                restoreAfterLogout = false;
                restoreAfterBlocking = false;
            }
        }
    }

    void onPostClientTick()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        final int previousDisplayedTab = displayedTab;
        refreshDisplayedTab();

        if (blockingInterfaceActive)
        {
            if (
                config.keepSidePanelOpen()
                    && validRestorablePanelTab(previousDisplayedTab)
            )
            {
                restoreAfterBlocking = true;
            }

            /* Never fight an interface which legitimately owns the side area. */
            restorePending = false;
            restoreDelay = 0;
            blockingLastTick = true;
            return;
        }

        if (blockingLastTick)
        {
            blockingLastTick = false;
            if (restoreAfterBlocking)
            {
                restoreAfterBlocking = false;
                requestLockRestore();
            }
        }

        final int selectedTab = client.getVarcIntValue(
            VarClientID.TOPLEVEL_PANEL
        );

        if (displayedTab == LayoutSpec.LOGOUT_TAB)
        {
            restorePending = false;
            restoreDelay = 0;
            restoreAfterLogout =
                config.keepSidePanelOpen()
                    && config.restoreLastSidePanel();
            return;
        }

        if (validRestorablePanelTab(displayedTab))
        {
            /*
             * This also accepts F-key/remapped/plugin-driven tab changes. A real
             * normal panel which is open is authoritative, regardless of source.
             */
            rememberTab(displayedTab);
            restorePending = false;
            restoreDelay = 0;
            restoreAfterLogout = false;
            return;
        }

        if (restoreAfterLogout && selectedTab != LayoutSpec.LOGOUT_TAB)
        {
            restoreAfterLogout = false;
            requestLifecycleRestore();
        }

        /*
         * Lock Panel Open also covers a normal panel which the game itself just
         * collapsed. Do this only after an actually displayed normal tab existed;
         * do not continuously force a panel open from arbitrary closed states.
         */
        if (
            config.keepSidePanelOpen()
                && validRestorablePanelTab(previousDisplayedTab)
                && selectedTab != LayoutSpec.LOGOUT_TAB
        )
        {
            requestLockRestore();
        }

        if (restoreDelay > 0)
        {
            restoreDelay--;
        }

        tryPendingRestore();
    }

    boolean isTabActive(int tabIndex)
    {
        return displayedTab == tabIndex;
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

        /*
         * This method is called only by OUR custom mouse buttons. Reconcile here
         * so a just-opened Bank/Shop/etc. cannot receive a queued custom click.
         * Keyboard and other plugins never pass through this method.
         */
        reconcileBlockingInterfaces();
        refreshDisplayedTab();

        if (shouldBlockCustomActivation(blockingInterfaceActive, tabIndex))
        {
            return;
        }

        restorePending = false;
        restoreDelay = 0;

        if (tabIndex == LayoutSpec.LOGOUT_TAB)
        {
            restoreAfterLogout =
                config.keepSidePanelOpen()
                    && config.restoreLastSidePanel();
            invokeNativeTabListener(layout, tabIndex);
            return;
        }

        restoreAfterLogout = false;

        final int selectedTab = client.getVarcIntValue(
            VarClientID.TOPLEVEL_PANEL
        );
        final boolean panelOpen = layout.isSidePanelOpen(client);

        /* Lock only the active custom tab itself; switching tabs remains valid. */
        if (
            config.keepSidePanelOpen()
                && panelOpen
                && displayedTab == tabIndex
        )
        {
            rememberTab(tabIndex);
            return;
        }

        /*
         * RuneScape can remember a selected tab after the normal side panel is
         * collapsed. Clear only that stale same-tab selection so the native OnOp
         * opens it rather than toggling it closed again.
         */
        if (selectedTab == tabIndex && !panelOpen)
        {
            client.setVarcIntValue(VarClientID.TOPLEVEL_PANEL, -1);
        }

        invokeNativeTabListener(layout, tabIndex);
    }

    private void refreshDisplayedTab()
    {
        final LayoutSpec layout = LayoutSpec.forRoot(
            client.getTopLevelInterfaceId()
        );

        if (layout == null)
        {
            displayedTab = -1;
            return;
        }

        final int selectedTab = client.getVarcIntValue(
            VarClientID.TOPLEVEL_PANEL
        );
        final boolean panelOpen = layout.isSidePanelOpen(client);

        displayedTab = resolveDisplayedTab(
            selectedTab,
            panelOpen,
            blockingInterfaceActive,
            inventoryOverrideActive
        );
    }

    static int resolveDisplayedTab(
        int selectedTab,
        boolean panelOpen,
        boolean blockingInterface,
        boolean inventoryOverride
    )
    {
        /* Logout is an explicit user-visible game tab and always wins. */
        if (selectedTab == LayoutSpec.LOGOUT_TAB)
        {
            return LayoutSpec.LOGOUT_TAB;
        }

        if (blockingInterface)
        {
            return inventoryOverride ? LayoutSpec.INVENTORY_TAB : -1;
        }

        return panelOpen && validRestorablePanelTab(selectedTab)
            ? selectedTab
            : -1;
    }

    static boolean shouldBlockCustomActivation(
        boolean blockingInterface,
        int tabIndex
    )
    {
        return blockingInterface && tabIndex != LayoutSpec.LOGOUT_TAB;
    }

    private void reconcileBlockingInterfaces()
    {
        activeBlockingGroups.clear();

        for (WidgetNode node : client.getComponentTable())
        {
            final int groupId = node.getId();
            if (LayoutSpec.isBlockingInterface(groupId))
            {
                activeBlockingGroups.add(groupId);
            }
        }

        updateBlockingFlags();
    }

    private void updateBlockingFlags()
    {
        boolean inventory = false;

        for (int groupId : activeBlockingGroups)
        {
            if (LayoutSpec.isInventoryOverrideInterface(groupId))
            {
                inventory = true;
                break;
            }
        }

        blockingInterfaceActive = !activeBlockingGroups.isEmpty();
        inventoryOverrideActive = inventory;
    }

    private void requestLockRestore()
    {
        if (!config.keepSidePanelOpen())
        {
            return;
        }

        restorePending = true;
        restoreDelay = RESTORE_DELAY_CLIENT_TICKS;
    }

    private void requestLifecycleRestore()
    {
        if (
            !config.keepSidePanelOpen()
                || !config.restoreLastSidePanel()
        )
        {
            return;
        }

        restorePending = true;
        restoreDelay = RESTORE_DELAY_CLIENT_TICKS;
    }

    private void tryPendingRestore()
    {
        if (!restorePending || restoreDelay > 0 || blockingInterfaceActive)
        {
            return;
        }

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

        if (selectedTab == LayoutSpec.LOGOUT_TAB)
        {
            return;
        }

        /* An externally opened normal panel wins and cancels our restoration. */
        refreshDisplayedTab();
        if (validRestorablePanelTab(displayedTab))
        {
            rememberTab(displayedTab);
            restorePending = false;
            return;
        }

        if (!validRestorablePanelTab(rememberedTab))
        {
            rememberedTab = LayoutSpec.INVENTORY_TAB;
        }

        if (selectedTab == rememberedTab)
        {
            client.setVarcIntValue(VarClientID.TOPLEVEL_PANEL, -1);
        }

        if (invokeNativeTabListener(layout, rememberedTab))
        {
            restorePending = false;
        }
    }

    private void resetTransientState()
    {
        activeBlockingGroups.clear();
        restorePending = false;
        restoreDelay = 0;
        restoreAfterLogout = false;
        restoreAfterBlocking = false;
        blockingLastTick = false;
        blockingInterfaceActive = false;
        inventoryOverrideActive = false;
        displayedTab = -1;
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

    private boolean invokeNativeTabListener(
        LayoutSpec layout,
        int tabIndex
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

        /*
         * Reuse RuneScape's original top-level tab listener. No menu dispatch,
         * keyboard remapping, or synthetic mouse/keyboard input is created here.
         */
        client.createScriptEventBuilder(onOpListener)
            .setSource(source)
            .setOp(1)
            .build()
            .run();
        return true;
    }

    private static boolean validRestorablePanelTab(int tabIndex)
    {
        return LayoutSpec.isValidTab(tabIndex)
            && tabIndex != LayoutSpec.LOGOUT_TAB;
    }
}
