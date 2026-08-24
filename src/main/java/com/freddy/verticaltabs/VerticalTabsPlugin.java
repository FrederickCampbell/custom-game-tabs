package com.freddy.verticaltabs;

import com.google.inject.Provides;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.PostClientTick;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
    name = "Custom Game Tabs",
    description = "Move, resize, arrange, and style Resizable Modern game tabs",
    tags = {"tabs", "interface", "ui", "movable", "customizable"},
    configName = VerticalTabsConfig.GROUP
)
public class VerticalTabsPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MouseManager mouseManager;

    @Inject
    private ConfigManager configManager;

    @Inject
    private TabIconRenderer iconRenderer;


    @Inject
    private LooseSnapManager snapManager;

    @Inject
    private SidePanelManager sidePanelManager;

    @Inject
    private VerticalTabsOverlay dockOverlay;

    @Inject
    private VanillaTabHider vanillaTabHider;

    @Inject
    private VerticalTabsConfig config;

    private final List<IndividualTabOverlay> looseOverlays =
        new ArrayList<>();

    private boolean mouseGestureCaptured;
    private boolean suppressNextClick;

    private final MouseAdapter mouseAdapter = new MouseAdapter()
    {
        @Override
        public MouseEvent mouseMoved(MouseEvent event)
        {
            updateHover(event.getX(), event.getY());
            return event;
        }

        @Override
        public MouseEvent mouseDragged(MouseEvent event)
        {
            if (mouseGestureCaptured)
            {
                event.consume();
                return event;
            }

            if (!event.isAltDown())
            {
                updateHover(event.getX(), event.getY());
            }

            return event;
        }

        @Override
        public MouseEvent mouseExited(MouseEvent event)
        {
            clearHover();
            return event;
        }

        @Override
        public MouseEvent mousePressed(MouseEvent event)
        {
            final int tabIndex = tabAt(
                event.getX(),
                event.getY()
            );

            if (event.isAltDown())
            {
                if (
                    config.moveSeparately()
                        && tabIndex >= 0
                )
                {
                    /*
                     * Isolate exactly the button being grabbed. The rest of
                     * the old cluster remains in place as clean branches.
                     */
                    snapManager.isolate(tabIndex);
                }

                return event;
            }

            if (tabIndex < 0)
            {
                suppressNextClick = false;
                /* A visible grouped frame is UI too; do not click through it. */
                if (inputSurfaceAt(event.getX(), event.getY()))
                {
                    mouseGestureCaptured = true;
                    suppressNextClick = true;
                    event.consume();
                }
                return event;
            }

            /*
             * Capture every non-Alt mouse button over a custom tab. Only left
             * click activates; right/middle clicks are intentionally swallowed
             * so RuneScape cannot open/interact with whatever sits behind it.
             */
            mouseGestureCaptured = true;
            suppressNextClick = true;
            event.consume();

            if (SwingUtilities.isLeftMouseButton(event))
            {
                clientThread.invoke(
                    () -> sidePanelManager.activateTab(tabIndex)
                );
            }

            return event;
        }

        @Override
        public MouseEvent mouseReleased(MouseEvent event)
        {
            if (mouseGestureCaptured)
            {
                mouseGestureCaptured = false;
                event.consume();
            }

            return event;
        }

        @Override
        public MouseEvent mouseClicked(MouseEvent event)
        {
            if (suppressNextClick)
            {
                suppressNextClick = false;
                event.consume();
                return event;
            }

            if (
                !event.isAltDown()
                    && inputSurfaceAt(event.getX(), event.getY())
            )
            {
                event.consume();
            }

            return event;
        }
    };

    private final MouseWheelListener mouseWheelListener = event ->
    {
        if (inputSurfaceAt(event.getX(), event.getY()))
        {
            event.consume();
        }

        return event;
    };

    @Override
    protected void startUp()
    {
        migrateLegacyConfiguration();
        sidePanelManager.startUp();
        mouseManager.registerMouseListener(mouseAdapter);
        mouseManager.registerMouseWheelListener(mouseWheelListener);
        rebuildOverlays();
        clientThread.invoke(vanillaTabHider::update);
    }

    @Override
    protected void shutDown()
    {
        mouseManager.unregisterMouseListener(mouseAdapter);
        mouseManager.unregisterMouseWheelListener(mouseWheelListener);
        mouseGestureCaptured = false;
        suppressNextClick = false;
        removeAllOverlays();
        clientThread.invoke(() ->
        {
            sidePanelManager.shutDown();
            vanillaTabHider.restoreAll();
        });
    }

    @Provides
    VerticalTabsConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(VerticalTabsConfig.class);
    }

    @Subscribe
    public void onPostClientTick(PostClientTick event)
    {
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            vanillaTabHider.update();
        }

        sidePanelManager.onPostClientTick();
        snapManager.onPostClientTick();
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        sidePanelManager.onWidgetLoaded(event.getGroupId());

        if (event.getGroupId() == InterfaceID.TOPLEVEL_PRE_EOC)
        {
            sidePanelManager.onTopLevelRebuilt();
        }
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event)
    {
        sidePanelManager.onWidgetClosed(
            event.getGroupId(),
            event.isUnload()
        );
    }

    @Subscribe
    public void onProfileChanged(ProfileChanged event)
    {
        /*
         * Run after OverlayManager has switched profiles and reloaded its own
         * saved positions. Rebuilding also applies this profile's move mode,
         * visible buttons, order, side-panel memory, and snap graph.
         */
        clientThread.invokeLater(() ->
        {
            sidePanelManager.startUp();
            vanillaTabHider.restoreAll();
            vanillaTabHider.update();
            rebuildOverlays();
        });
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        sidePanelManager.onGameStateChanged(event.getGameState());

        if (event.getGameState() == GameState.LOGGED_IN)
        {
            clientThread.invokeLater(vanillaTabHider::update);
        }
        else if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            vanillaTabHider.restoreAll();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!VerticalTabsConfig.GROUP.equals(event.getGroup()))
        {
            return;
        }

        clientThread.invoke(() ->
        {
            vanillaTabHider.restoreAll();
            vanillaTabHider.update();
            sidePanelManager.onConfigChanged(event.getKey());

            if (requiresOverlayRebuild(event.getKey()))
            {
                rebuildOverlays();
            }
            else
            {
                revalidateOverlays();
                snapManager.configChanged(event.getKey());
            }
        });
    }

    private static boolean requiresOverlayRebuild(String key)
    {
        return "moveSeparately".equals(key)
            || "tabOrder".equals(key)
            || (key != null && key.startsWith("show"));
    }

    private void rebuildOverlays()
    {
        removeAllOverlays();
        clearHover();

        if (config.moveSeparately())
        {
            for (int tabIndex : TabLayout.visibleOrder(config))
            {
                final IndividualTabOverlay overlay =
                    new IndividualTabOverlay(
                        client,
                        config,
                        iconRenderer,
                        sidePanelManager,
                        snapManager,
                        tabIndex
                    );

                looseOverlays.add(overlay);
                overlayManager.add(overlay);
            }

            snapManager.setOverlays(looseOverlays);
        }
        else
        {
            snapManager.clearOverlays();
            dockOverlay.configurationChanged();
            overlayManager.add(dockOverlay);
        }
    }

    private void revalidateOverlays()
    {
        dockOverlay.configurationChanged();

        for (IndividualTabOverlay overlay : looseOverlays)
        {
            overlay.revalidate();
        }
    }

    private void removeAllOverlays()
    {
        overlayManager.remove(dockOverlay);

        for (IndividualTabOverlay overlay : looseOverlays)
        {
            overlayManager.remove(overlay);
        }

        looseOverlays.clear();
        snapManager.clearOverlays();
    }

    private int tabAt(int x, int y)
    {
        if (!config.moveSeparately())
        {
            return dockOverlay.tabAt(x, y);
        }

        /*
         * Later-added overlays are rendered above earlier ones. Hit-test in
         * reverse so overlapping buttons detach/click the same topmost button
         * RuneLite presents to the user.
         */
        for (int index = looseOverlays.size() - 1; index >= 0; index--)
        {
            final IndividualTabOverlay overlay =
                looseOverlays.get(index);

            if (overlay.contains(x, y))
            {
                return overlay.getTabIndex();
            }
        }

        return -1;
    }

    private boolean inputSurfaceAt(int x, int y)
    {
        if (config.moveSeparately())
        {
            return tabAt(x, y) >= 0;
        }

        return dockOverlay.containsInputSurface(x, y);
    }

    private void updateHover(int x, int y)
    {
        if (!config.moveSeparately())
        {
            dockOverlay.onMouseMoved(x, y);
            return;
        }

        final int hoveredTab = tabAt(x, y);

        for (IndividualTabOverlay overlay : looseOverlays)
        {
            overlay.setHovered(
                overlay.getTabIndex() == hoveredTab
            );
        }
    }

    private void clearHover()
    {
        dockOverlay.onMouseExited();

        for (IndividualTabOverlay overlay : looseOverlays)
        {
            overlay.setHovered(false);
        }
    }

    private void migrateLegacyConfiguration()
    {
        migrateColumns();
        migrateHiddenButtons();
        migrateMoveMode();
        migrateButtonScale();
        migrateDefaultColors();
        removeObsoleteInterfaceScaleSettings();
    }


    private void removeObsoleteInterfaceScaleSettings()
    {
        final String[] keys =
        {
            "scaleSidePanel",
            "sidePanelScale",
            "useGlobalUiScale",
            "globalUiMagnification"
        };

        for (String key : keys)
        {
            configManager.unsetConfiguration(
                VerticalTabsConfig.GROUP,
                key
            );
        }
    }

    private void migrateColumns()
    {
        if (
            configManager.getConfiguration(
                VerticalTabsConfig.GROUP,
                "buttonsAcross"
            ) != null
        )
        {
            return;
        }

        final String oldColumns =
            configManager.getConfiguration(
                VerticalTabsConfig.GROUP,
                "columns"
            );

        if ("ONE".equalsIgnoreCase(oldColumns))
        {
            configManager.setConfiguration(
                VerticalTabsConfig.GROUP,
                "buttonsAcross",
                1
            );
        }
        else if ("TWO".equalsIgnoreCase(oldColumns))
        {
            configManager.setConfiguration(
                VerticalTabsConfig.GROUP,
                "buttonsAcross",
                2
            );
        }
        else if (
            oldColumns != null
                && oldColumns.matches("\\d+")
        )
        {
            configManager.setConfiguration(
                VerticalTabsConfig.GROUP,
                "buttonsAcross",
                oldColumns
            );
        }
    }

    private void migrateHiddenButtons()
    {
        final String hidden =
            configManager.getConfiguration(
                VerticalTabsConfig.GROUP,
                "hiddenTabs"
            );

        if (hidden == null || hidden.isBlank())
        {
            return;
        }

        for (int tabIndex : TabLayout.parse(hidden))
        {
            final String key = TabLayout.showKey(tabIndex);

            if (
                configManager.getConfiguration(
                    VerticalTabsConfig.GROUP,
                    key
                ) == null
            )
            {
                configManager.setConfiguration(
                    VerticalTabsConfig.GROUP,
                    key,
                    false
                );
            }
        }
    }

    private void migrateMoveMode()
    {
        if (
            configManager.getConfiguration(
                VerticalTabsConfig.GROUP,
                "moveSeparately"
            ) != null
        )
        {
            return;
        }

        final String oldMode =
            configManager.getConfiguration(
                VerticalTabsConfig.GROUP,
                "layoutStyle"
            );

        configManager.setConfiguration(
            VerticalTabsConfig.GROUP,
            "moveSeparately",
            "MOVE_ONE_BY_ONE".equalsIgnoreCase(oldMode)
        );
    }

    private void migrateButtonScale()
    {
        final String raw =
            configManager.getConfiguration(
                VerticalTabsConfig.GROUP,
                "buttonScale"
            );

        if (raw != null && raw.matches("\\d+"))
        {
            final int value = Math.max(
                25,
                Math.min(200, Integer.parseInt(raw))
            );

            if (!raw.equals(Integer.toString(value)))
            {
                configManager.setConfiguration(
                    VerticalTabsConfig.GROUP,
                    "buttonScale",
                    value
                );
            }

            return;
        }

        if (raw != null && raw.matches("PERCENT_\\d+"))
        {
            final int value = Math.max(
                25,
                Math.min(
                    200,
                    Integer.parseInt(
                        raw.substring("PERCENT_".length())
                    )
                )
            );

            configManager.setConfiguration(
                VerticalTabsConfig.GROUP,
                "buttonScale",
                value
            );
            return;
        }

        final String oldSize =
            configManager.getConfiguration(
                VerticalTabsConfig.GROUP,
                "buttonSize"
            );

        int legacyPixels = 42;

        if (oldSize != null)
        {
            try
            {
                legacyPixels = Integer.parseInt(oldSize);
            }
            catch (NumberFormatException ignored)
            {
                legacyPixels = 42;
            }
        }

        final int estimatedPercent = Math.round(
            legacyPixels * 100.0f / 42.0f
        );

        configManager.setConfiguration(
            VerticalTabsConfig.GROUP,
            "buttonScale",
            Math.max(25, Math.min(200, estimatedPercent))
        );
    }


    private void migrateDefaultColors()
    {
        migrateDefaultColor(
            "buttonColor",
            new java.awt.Color(41, 38, 32, 230),
            new java.awt.Color(49, 38, 26, 238)
        );
        migrateDefaultColor(
            "hoverColor",
            new java.awt.Color(75, 68, 54, 240),
            new java.awt.Color(82, 62, 36, 246)
        );
        migrateDefaultColor(
            "selectedColor",
            new java.awt.Color(108, 88, 45, 245),
            new java.awt.Color(112, 82, 35, 250)
        );
        migrateDefaultColor(
            "borderColor",
            new java.awt.Color(154, 128, 72, 235),
            new java.awt.Color(151, 113, 49, 248)
        );
    }

    private void migrateDefaultColor(
        String key,
        java.awt.Color oldDefault,
        java.awt.Color newDefault
    )
    {
        final String raw = configManager.getConfiguration(
            VerticalTabsConfig.GROUP,
            key
        );

        if (
            raw == null
                || raw.equals(Integer.toString(oldDefault.getRGB()))
        )
        {
            configManager.setConfiguration(
                VerticalTabsConfig.GROUP,
                key,
                newDefault
            );
        }
    }

}
