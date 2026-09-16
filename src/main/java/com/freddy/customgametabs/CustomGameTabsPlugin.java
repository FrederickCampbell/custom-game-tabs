package com.freddy.customgametabs;

import com.google.inject.Provides;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.PostClientTick;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
    name = "Custom Game Tabs",
    description = "Move, resize, arrange, and style Resizable Modern game tabs",
    tags = {"tabs", "interface", "ui", "movable", "customizable"},
    configName = CustomGameTabsConfig.GROUP
)
public class CustomGameTabsPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(CustomGameTabsPlugin.class);

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private OverlayManager overlayManager;
    @Inject private ClientToolbar clientToolbar;
    @Inject private MouseManager mouseManager;
    @Inject private ConfigManager configManager;
    @Inject private TabIconRenderer iconRenderer;
    @Inject private TabSnapManager snapManager;
    @Inject private TabTooltipPresenter tooltipPresenter;
    @Inject private ActivityLayoutStore activityLayoutStore;
    @Inject private NativeTabController nativeTabController;
    @Inject private CustomGameTabsOverlay dockOverlay;
    @Inject private DrawerOverlay drawerOverlay;
    @Inject private NativeTabPresentation nativeTabPresentation;
    @Inject private NativeTabMenuBridge nativeTabMenuBridge;
    @Inject private CustomGameTabsConfig config;

    private final List<CustomGameTabOverlay> looseOverlays = new ArrayList<>();

    private TabLayoutsPanel layoutsPanel;
    private NavigationButton layoutsNavigation;
    private StickyGroups stickyGroups = new StickyGroups();
    private boolean drawerExpanded;
    private boolean applyingActivityLayout;
    private int restoreWorkingCountdown;
    private int lastCanvasWidth = -1;
    private int lastCanvasHeight = -1;

    private boolean leftGestureCaptured;
    private boolean suppressNextLeftClick;

    /* Alt-drag bookkeeping. CGT owns the entire Freeform gesture so RuneLite's
     * generic overlay mover cannot independently re-anchor one stone or leak the
     * same Alt-drag through to game-camera input. */
    private boolean altDragGesture;
    private boolean altDragDrawer;
    private int altDragTab = -1;
    private int dragStartMouseX;
    private int dragStartMouseY;
    private Point dragStartDrawerHandle;
    private Map<Integer, Point> dragStartLocations = new LinkedHashMap<>();

    /* AWT callbacks must not query Widget state. Publish client-thread bounds. */
    private volatile Rectangle[] sidePanelBounds = new Rectangle[0];

    private final MouseAdapter mouseAdapter = new MouseAdapter()
    {
        @Override
        public MouseEvent mouseMoved(MouseEvent event)
        {
            if (!client.isMenuOpen())
            {
                updateHover(event.getX(), event.getY());
            }
            return event;
        }

        @Override
        public MouseEvent mouseDragged(MouseEvent event)
        {
            if (client.isMenuOpen())
            {
                return event;
            }

            if (leftGestureCaptured)
            {
                event.consume();
                return event;
            }

            if (altDragGesture)
            {
                previewAltDrag(event.getX(), event.getY());
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
            if (!client.isMenuOpen())
            {
                clearHover();
            }
            return event;
        }

        @Override
        public MouseEvent mousePressed(MouseEvent event)
        {
            if (client.isMenuOpen())
            {
                resetClickGesture();
                resetAltDrag();
                return event;
            }

            if (event.isAltDown() && SwingUtilities.isLeftMouseButton(event))
            {
                beginAltDrag(event.getX(), event.getY());
                if (altDragGesture)
                {
                    suppressNextLeftClick = true;
                    event.consume();
                }
                return event;
            }

            resetAltDrag();
            final int x = event.getX();
            final int y = event.getY();
            if (!ownsInputSurface(x, y) || !SwingUtilities.isLeftMouseButton(event))
            {
                return event;
            }

            leftGestureCaptured = true;
            suppressNextLeftClick = true;
            event.consume();

            if (drawerOverlay.handleContains(x, y))
            {
                clientThread.invoke(CustomGameTabsPlugin.this::toggleDrawer);
                return event;
            }

            final int tabIndex = tabAt(x, y);
            if (tabIndex >= 0)
            {
                final boolean fromDrawer = drawerOverlay.tabAt(x, y) == tabIndex;
                clientThread.invoke(() ->
                {
                    nativeTabController.activateTab(tabIndex);
                    if (fromDrawer && config.drawerCloseAfterSelection())
                    {
                        setDrawerExpanded(false, true);
                    }
                });
            }
            return event;
        }

        @Override
        public MouseEvent mouseReleased(MouseEvent event)
        {
            if (altDragGesture)
            {
                final boolean drawer = altDragDrawer;
                final int tab = altDragTab;
                final int startX = dragStartMouseX;
                final int startY = dragStartMouseY;
                final int endX = event.getX();
                final int endY = event.getY();
                final Point drawerStart = dragStartDrawerHandle == null
                    ? null
                    : new Point(dragStartDrawerHandle);
                final Map<Integer, Point> starts = copyPoints(dragStartLocations);
                resetAltDrag();

                clientThread.invokeLater(() -> finishAltDrag(
                    drawer,
                    tab,
                    startX,
                    startY,
                    endX,
                    endY,
                    drawerStart,
                    starts
                ));
                event.consume();
                return event;
            }

            if (client.isMenuOpen())
            {
                leftGestureCaptured = false;
                return event;
            }

            if (leftGestureCaptured && SwingUtilities.isLeftMouseButton(event))
            {
                leftGestureCaptured = false;
                event.consume();
            }
            return event;
        }

        @Override
        public MouseEvent mouseClicked(MouseEvent event)
        {
            if (client.isMenuOpen())
            {
                suppressNextLeftClick = false;
                return event;
            }

            if (suppressNextLeftClick && SwingUtilities.isLeftMouseButton(event))
            {
                suppressNextLeftClick = false;
                event.consume();
            }
            return event;
        }
    };

    @Override
    protected void startUp()
    {
        migrateLegacyConfiguration();
        nativeTabController.startUp();
        /* Run before RuneLite's generic OverlayRenderer. Consuming an owned
         * Alt-drag here prevents both native overlay dragging and camera input. */
        mouseManager.registerMouseListener(0, mouseAdapter);
        log.info("[CGT 2.2.2] input listener registered at position 0");

        final Dimension startupDimensions = client.getRealDimensions();
        lastCanvasWidth = startupDimensions.width;
        lastCanvasHeight = startupDimensions.height;

        final ActivityLayoutSnapshot working = activityLayoutStore.loadWorking();
        if (working != null)
        {
            applyActivitySettings(working);
        }
        else
        {
            drawerExpanded = false;
            stickyGroups = new StickyGroups();
        }

        final Map<Integer, Point> positions = working != null
            && config.layoutMode() == TabLayoutMode.FREEFORM
            ? resolveLocations(working)
            : null;
        final Point drawerHandle = working != null
            && config.layoutMode() == TabLayoutMode.FREEFORM
            ? resolveDrawerHandle(working)
            : null;

        rebuildOverlays(positions, drawerHandle);
        addLayoutsNavigation();
        clientThread.invoke(nativeTabPresentation::update);
        restoreWorkingCountdown = client.getGameState() == GameState.LOGGED_IN ? 2 : 0;
    }

    @Override
    protected void shutDown()
    {
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            saveWorkingSnapshot();
        }

        mouseManager.unregisterMouseListener(mouseAdapter);
        resetClickGesture();
        resetAltDrag();
        sidePanelBounds = new Rectangle[0];
        removeAllOverlays();
        removeLayoutsNavigation();
        clientThread.invoke(() ->
        {
            nativeTabController.shutDown();
            nativeTabPresentation.restoreAll();
        });
    }

    @Provides
    CustomGameTabsConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(CustomGameTabsConfig.class);
    }

    @Override
    public void resetConfiguration()
    {
        activityLayoutStore.clearAll();
        stickyGroups = new StickyGroups();
        drawerExpanded = false;
        refreshLayoutsPanel();
    }

    @Subscribe
    public void onPostClientTick(PostClientTick event)
    {
        handleCanvasResize();
        refreshInputGeometry();

        if (client.getGameState() == GameState.LOGGED_IN)
        {
            nativeTabPresentation.update();

            if (config.layoutMode() == TabLayoutMode.DOCKED)
            {
                positionDrawerForDock();
            }

            if (restoreWorkingCountdown > 0)
            {
                restoreWorkingCountdown--;
                if (restoreWorkingCountdown == 0)
                {
                    restoreWorkingAfterLoadScreen();
                }
            }

            if (!activityLayoutStore.hasWorking() && hasRenderableLayout())
            {
                saveWorkingSnapshot();
            }
        }

        nativeTabController.onPostClientTick();
    }

    @Subscribe(priority = -100)
    public void onPostMenuSort(PostMenuSort event)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen())
        {
            return;
        }

        final net.runelite.api.Point mouse = client.getMouseCanvasPosition();
        final int x = mouse.getX();
        final int y = mouse.getY();
        nativeTabMenuBridge.prepareMenu(tabAt(x, y), ownsInputSurface(x, y));
    }

    @Subscribe
    public void onVarClientIntChanged(VarClientIntChanged event)
    {
        if (event.getIndex() == VarClientID.TOPLEVEL_PANEL)
        {
            nativeTabController.onTopLevelPanelChanged();
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == InterfaceID.TOPLEVEL_PRE_EOC)
        {
            nativeTabPresentation.restoreAll();
            nativeTabController.onTopLevelRebuilt();
            clientThread.invokeLater(nativeTabPresentation::update);
            if (client.getGameState() == GameState.LOGGED_IN)
            {
                restoreWorkingCountdown = Math.max(restoreWorkingCountdown, 1);
            }
        }
    }

    @Subscribe
    public void onProfileChanged(ProfileChanged event)
    {
        clientThread.invokeLater(() ->
        {
            nativeTabController.startUp();
            nativeTabPresentation.restoreAll();
            nativeTabPresentation.update();

            final ActivityLayoutSnapshot working = activityLayoutStore.loadWorking();
            if (working != null)
            {
                applyActivitySettings(working);
            }
            else
            {
                drawerExpanded = false;
                stickyGroups = new StickyGroups();
            }

            rebuildOverlays(
                working != null && config.layoutMode() == TabLayoutMode.FREEFORM
                    ? resolveLocations(working)
                    : null,
                working != null && config.layoutMode() == TabLayoutMode.FREEFORM
                    ? resolveDrawerHandle(working)
                    : null
            );
            restoreWorkingCountdown = client.getGameState() == GameState.LOGGED_IN ? 2 : 0;
            refreshLayoutsPanel();
        });
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        final GameState state = event.getGameState();

        /* The working snapshot is already updated whenever the user changes the
         * layout (drag, preset, state/order, drawer, etc.). Do NOT recapture it
         * while entering a loading/hop state: RuneLite may already be rebuilding
         * overlay geometry at that point, and saving then can overwrite the good
         * working layout with a transient default/top-right anchor. */
        if (state != GameState.LOGGED_IN)
        {
            restoreWorkingCountdown = 0;
            sidePanelBounds = new Rectangle[0];
        }

        nativeTabController.onGameStateChanged(state);

        if (state == GameState.LOGGED_IN)
        {
            /* Do not restore during GameStateChanged itself. Wait for the rebuilt
             * top-level interface and overlay bounds to settle, then reapply the
             * canonical working snapshot. */
            restoreWorkingCountdown = 2;
        }
        else if (state == GameState.LOGIN_SCREEN)
        {
            nativeTabPresentation.restoreAll();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!CustomGameTabsConfig.GROUP.equals(event.getGroup()) || applyingActivityLayout)
        {
            return;
        }

        final String key = event.getKey();
        final Map<Integer, Point> positions = currentFreeformLocations();
        final Point handle = drawerOverlay.getHandleLocation();
        final Map<Integer, Point> dockSeed = "layoutMode".equals(key)
            && config.layoutMode() == TabLayoutMode.FREEFORM
            ? dockOverlay.captureButtonLocations()
            : new LinkedHashMap<>();

        clientThread.invoke(() ->
        {
            nativeTabPresentation.update();

            if ("layoutMode".equals(key))
            {
                handleLayoutModeChange(dockSeed, handle);
            }
            else if (requiresOverlayRebuild(key))
            {
                stickyGroups = stickyGroups.retain(TabLayout.mainOrder(config));
                rebuildOverlays(seedMissingMainLocations(positions), handle);
                saveWorkingSnapshot();
            }
            else
            {
                revalidateOverlays();
                updateSnapAvailability();
                if (config.layoutMode() == TabLayoutMode.DOCKED)
                {
                    positionDrawerForDock();
                }
                else
                {
                    repositionFreeformDrawer(handle);
                }
                saveWorkingSnapshot();
            }

            refreshLayoutsPanel();
        });
    }

    private static boolean requiresOverlayRebuild(String key)
    {
        return "tabOrder".equals(key)
            || "drawerTabs".equals(key)
            || (key != null && key.startsWith("show"));
    }

    private void handleLayoutModeChange(Map<Integer, Point> dockSeed, Point drawerHandle)
    {
        if (config.layoutMode() == TabLayoutMode.FREEFORM)
        {
            final Map<Integer, Point> seed = dockSeed == null || dockSeed.isEmpty()
                ? seedMissingMainLocations(currentFreeformLocations())
                : dockSeed;
            rebuildOverlays(seedMissingMainLocations(seed), drawerHandle);
        }
        else
        {
            if (!looseOverlays.isEmpty())
            {
                saveRecoverySnapshot();
            }
            rebuildOverlays(null, drawerHandle);
        }
        saveWorkingSnapshot();
    }

    private void restoreWorkingAfterLoadScreen()
    {
        final ActivityLayoutSnapshot working = activityLayoutStore.loadWorking();
        if (working == null)
        {
            return;
        }

        /* A loading screen may cause RuneLite to re-anchor the live Overlay
         * instances. Reapply our canonical working coordinates after the new
         * top-level interface exists, rather than requiring the user to press
         * Load again. */
        if (config.layoutMode() == TabLayoutMode.FREEFORM)
        {
            applyLocations(resolveLocations(working));
            final Point handle = resolveDrawerHandle(working);
            if (handle != null)
            {
                repositionFreeformDrawer(handle);
            }
        }
        else
        {
            positionDrawerForDock();
        }
    }

    private void rebuildOverlays(Map<Integer, Point> forcedLocations, Point drawerHandle)
    {
        final Point previousHandle = drawerHandle != null
            ? new Point(drawerHandle)
            : drawerOverlay.getHandleLocation();

        removeAllOverlays();
        clearHover();

        if (config.layoutMode() == TabLayoutMode.FREEFORM)
        {
            for (int tabIndex : TabLayout.mainOrder(config))
            {
                final CustomGameTabOverlay overlay = new CustomGameTabOverlay(
                    client,
                    config,
                    iconRenderer,
                    nativeTabController,
                    snapManager,
                    tooltipPresenter,
                    tabIndex
                );
                /* CGT owns Freeform drop/snap in finishAltDrag(). Do not let
                 * OverlayRenderer run a second, independent onDrag snap path. */
                overlay.setCustomSnapEnabled(false);
                looseOverlays.add(overlay);
                overlayManager.add(overlay);

                final Point location = forcedLocations == null ? null : forcedLocations.get(tabIndex);
                if (location != null)
                {
                    placeOverlay(overlay, location);
                }
            }
            stickyGroups = stickyGroups.retain(TabLayout.mainOrder(config));
        }
        else
        {
            dockOverlay.configurationChanged();
            overlayManager.add(dockOverlay);
        }

        drawerOverlay.setFreeformMovable(config.layoutMode() == TabLayoutMode.FREEFORM);
        drawerOverlay.setExpanded(drawerExpanded);
        overlayManager.add(drawerOverlay);

        if (config.layoutMode() == TabLayoutMode.FREEFORM)
        {
            final Point handle = previousHandle != null
                ? previousHandle
                : defaultFreeformDrawerHandle(
                    forcedLocations == null ? currentFreeformLocations() : forcedLocations
                );
            repositionFreeformDrawer(handle);
        }
        else
        {
            positionDrawerForDock();
        }
    }

    private void revalidateOverlays()
    {
        dockOverlay.configurationChanged();
        drawerOverlay.revalidate();
        for (CustomGameTabOverlay overlay : looseOverlays)
        {
            overlay.revalidate();
        }
    }

    private void removeAllOverlays()
    {
        overlayManager.remove(dockOverlay);
        overlayManager.remove(drawerOverlay);
        for (CustomGameTabOverlay overlay : looseOverlays)
        {
            overlayManager.remove(overlay);
        }
        looseOverlays.clear();
    }

    private int tabAt(int x, int y)
    {
        final int drawerTab = drawerOverlay.tabAt(x, y);
        if (drawerTab >= 0)
        {
            return drawerTab;
        }

        if (config.layoutMode() == TabLayoutMode.DOCKED)
        {
            return dockOverlay.tabAt(x, y);
        }

        for (int index = looseOverlays.size() - 1; index >= 0; index--)
        {
            final CustomGameTabOverlay overlay = looseOverlays.get(index);
            if (overlay.contains(x, y))
            {
                return overlay.getTabIndex();
            }
        }
        return -1;
    }

    private boolean ownsInputSurface(int x, int y)
    {
        for (Rectangle bounds : sidePanelBounds)
        {
            if (bounds.contains(x, y))
            {
                return false;
            }
        }

        if (drawerOverlay.containsInputSurface(x, y))
        {
            return true;
        }

        return config.layoutMode() == TabLayoutMode.DOCKED
            ? dockOverlay.containsInputSurface(x, y)
            : tabAt(x, y) >= 0;
    }

    private void refreshInputGeometry()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            sidePanelBounds = new Rectangle[0];
            return;
        }

        final LayoutSpec layout = LayoutSpec.forRoot(client.getTopLevelInterfaceId());
        sidePanelBounds = layout == null
            ? new Rectangle[0]
            : layout.captureSidePanelBounds(client);
    }

    private void updateHover(int x, int y)
    {
        if (!ownsInputSurface(x, y))
        {
            clearHover();
            return;
        }

        drawerOverlay.onMouseMoved(x, y);
        if (drawerOverlay.containsInputSurface(x, y))
        {
            dockOverlay.onMouseExited();
            for (CustomGameTabOverlay overlay : looseOverlays)
            {
                overlay.setHovered(false);
            }
            return;
        }

        if (config.layoutMode() == TabLayoutMode.DOCKED)
        {
            dockOverlay.onMouseMoved(x, y);
            return;
        }

        final int hoveredTab = tabAt(x, y);
        for (CustomGameTabOverlay overlay : looseOverlays)
        {
            overlay.setHovered(overlay.getTabIndex() == hoveredTab);
        }
    }

    private void clearHover()
    {
        dockOverlay.onMouseExited();
        drawerOverlay.onMouseExited();
        for (CustomGameTabOverlay overlay : looseOverlays)
        {
            overlay.setHovered(false);
        }
    }

    private void beginAltDrag(int x, int y)
    {
        resetAltDrag();
        if (config.layoutMode() != TabLayoutMode.FREEFORM || !ownsInputSurface(x, y))
        {
            return;
        }

        altDragGesture = true;
        dragStartMouseX = x;
        dragStartMouseY = y;

        if (drawerOverlay.containsInputSurface(x, y))
        {
            altDragDrawer = true;
            dragStartDrawerHandle = drawerOverlay.getHandleLocation();
            if (dragStartDrawerHandle != null)
            {
                placeDrawerHandle(dragStartDrawerHandle, false);
            }
            log.info("[CGT 2.2.2] begin drawer drag handle={}", dragStartDrawerHandle);
            return;
        }

        final int tab = tabAt(x, y);
        final CustomGameTabOverlay moving = overlayForTab(tab);
        if (moving == null)
        {
            resetAltDrag();
            return;
        }

        altDragTab = tab;
        final Map<Integer, Point> current = currentFreeformLocations();
        if (config.stickTogether())
        {
            /* Geometry is authoritative. This picks up a tab snapped/touched
             * after Stick Together was originally enabled. */
            stickyGroups = StickyGroups.detect(
                current,
                DockMetrics.buttonCellSize(config),
                DockMetrics.gap(config)
            );
        }

        final List<Integer> members = config.stickTogether()
            ? stickyGroups.membersFor(tab)
            : java.util.Collections.singletonList(tab);
        for (int member : members)
        {
            final Point point = current.get(member);
            final CustomGameTabOverlay overlay = overlayForTab(member);
            if (point != null && overlay != null)
            {
                final Point canonical = clampTabLocation(point);
                dragStartLocations.put(member, new Point(canonical));
                canonicalizeOverlayForDrag(overlay, canonical);
            }
        }

        if (dragStartLocations.isEmpty())
        {
            resetAltDrag();
            return;
        }

        log.info(
            "[CGT 2.2.2] begin tab drag tab={} stick={} members={}",
            tab,
            config.stickTogether(),
            dragStartLocations.keySet()
        );
    }

    private void previewAltDrag(int x, int y)
    {
        if (!altDragGesture)
        {
            return;
        }

        final int dx = x - dragStartMouseX;
        final int dy = y - dragStartMouseY;

        if (altDragDrawer)
        {
            if (dragStartDrawerHandle != null)
            {
                repositionFreeformDrawerPreview(
                    clampHandle(new Point(
                        dragStartDrawerHandle.x + dx,
                        dragStartDrawerHandle.y + dy
                    ))
                );
            }
            return;
        }

        if (altDragTab < 0 || dragStartLocations.isEmpty())
        {
            return;
        }

        final Point delta = clampedGroupDelta(dx, dy, dragStartLocations);
        for (Map.Entry<Integer, Point> entry : dragStartLocations.entrySet())
        {
            final CustomGameTabOverlay overlay = overlayForTab(entry.getKey());
            if (overlay != null)
            {
                overlay.setPreferredPosition(null);
                overlay.setPreferredLocation(new Point(
                    entry.getValue().x + delta.x,
                    entry.getValue().y + delta.y
                ));
                overlay.revalidate();
            }
        }
    }

    private void finishAltDrag(
        boolean drawer,
        int tab,
        int startX,
        int startY,
        int endX,
        int endY,
        Point drawerStart,
        Map<Integer, Point> starts
    )
    {
        if (config.layoutMode() != TabLayoutMode.FREEFORM)
        {
            return;
        }

        final int dx = endX - startX;
        final int dy = endY - startY;

        if (drawer)
        {
            if (drawerStart != null)
            {
                final Point handle = clampHandle(new Point(drawerStart.x + dx, drawerStart.y + dy));
                repositionFreeformDrawer(handle);
                log.info("[CGT 2.2.2] finish drawer drag handle={}", handle);
                saveWorkingSnapshot();
            }
            return;
        }

        final CustomGameTabOverlay moving = overlayForTab(tab);
        if (moving == null || starts.isEmpty())
        {
            return;
        }

        Point delta = clampedGroupDelta(dx, dy, starts);
        Map<Integer, Point> proposed = translatedLocations(starts, delta);

        if (config.snapLooseButtons())
        {
            final Point movingPoint = proposed.get(tab);
            if (movingPoint != null)
            {
                final int cell = DockMetrics.buttonCellSize(config);
                final TabSnapManager.SnapResult snap = snapManager.findSnap(
                    tab,
                    movingPoint,
                    cell,
                    looseOverlays,
                    starts.keySet()
                );
                if (snap != null)
                {
                    final int snapDx = snap.location.x - movingPoint.x;
                    final int snapDy = snap.location.y - movingPoint.y;
                    delta = clampedGroupDelta(
                        delta.x + snapDx,
                        delta.y + snapDy,
                        starts
                    );
                    proposed = translatedLocations(starts, delta);
                    log.info(
                        "[CGT 2.2.2] snap tab={} target={} members={} desired={}",
                        tab,
                        snap.targetTab,
                        starts.keySet(),
                        snap.location
                    );
                }
            }
        }

        for (Map.Entry<Integer, Point> entry : proposed.entrySet())
        {
            final CustomGameTabOverlay overlay = overlayForTab(entry.getKey());
            if (overlay != null)
            {
                placeOverlay(overlay, entry.getValue());
            }
        }

        final Map<Integer, Point> finalLocations = currentFreeformLocations();
        finalLocations.putAll(copyPoints(proposed));
        stickyGroups = config.stickTogether()
            ? StickyGroups.detect(
                finalLocations,
                DockMetrics.buttonCellSize(config),
                DockMetrics.gap(config)
            )
            : new StickyGroups();

        log.info(
            "[CGT 2.2.2] finish tab drag tab={} members={} delta={} groups={}",
            tab,
            starts.keySet(),
            delta,
            stickyGroups.asLists()
        );
        saveWorkingSnapshot();
    }

    private void canonicalizeOverlayForDrag(CustomGameTabOverlay overlay, Point location)
    {
        overlayManager.resetOverlay(overlay);
        overlay.setPreferredPosition(null);
        overlay.setPreferredLocation(new Point(location));
        overlay.revalidate();
    }

    private static Map<Integer, Point> translatedLocations(
        Map<Integer, Point> starts,
        Point delta
    )
    {
        final Map<Integer, Point> translated = new LinkedHashMap<>();
        for (Map.Entry<Integer, Point> entry : starts.entrySet())
        {
            translated.put(
                entry.getKey(),
                new Point(entry.getValue().x + delta.x, entry.getValue().y + delta.y)
            );
        }
        return translated;
    }

    private Point clampedGroupDelta(int dx, int dy, Map<Integer, Point> starts)
    {
        final Dimension dimensions = client.getRealDimensions();
        final int cell = DockMetrics.buttonCellSize(config);
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Point point : starts.values())
        {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x + cell);
            maxY = Math.max(maxY, point.y + cell);
        }

        if (minX == Integer.MAX_VALUE)
        {
            return new Point(0, 0);
        }

        final int clampedX = clamp(dx, -minX, Math.max(-minX, dimensions.width - maxX));
        final int clampedY = clamp(dy, -minY, Math.max(-minY, dimensions.height - maxY));
        return new Point(clampedX, clampedY);
    }

    private void resetAltDrag()
    {
        altDragGesture = false;
        altDragDrawer = false;
        altDragTab = -1;
        dragStartDrawerHandle = null;
        dragStartLocations = new LinkedHashMap<>();
    }

    private void resetClickGesture()
    {
        leftGestureCaptured = false;
        suppressNextLeftClick = false;
    }

    private CustomGameTabOverlay overlayForTab(int tabIndex)
    {
        for (CustomGameTabOverlay overlay : looseOverlays)
        {
            if (overlay.getTabIndex() == tabIndex)
            {
                return overlay;
            }
        }
        return null;
    }

    private void updateSnapAvailability()
    {
        /* Freeform snap is handled once, by finishAltDrag(), so Snap Buttons and
         * Stick Together can coexist without RuneLite's native drag path racing it. */
        for (CustomGameTabOverlay overlay : looseOverlays)
        {
            overlay.setCustomSnapEnabled(false);
        }
    }

    private void toggleDrawer()
    {
        setDrawerExpanded(!drawerExpanded, true);
    }

    private void setDrawerExpanded(boolean expanded, boolean save)
    {
        final Point anchor = drawerOverlay.getHandleLocation();
        drawerExpanded = expanded;
        drawerOverlay.setExpanded(expanded);

        if (config.layoutMode() == TabLayoutMode.FREEFORM)
        {
            repositionFreeformDrawer(
                anchor == null ? defaultFreeformDrawerHandle(currentFreeformLocations()) : anchor
            );
        }
        else
        {
            positionDrawerForDock();
        }

        log.info("[CGT 2.2.2] drawer expanded={} handle={}", expanded, drawerOverlay.getHandleLocation());

        if (save)
        {
            saveWorkingSnapshot();
            refreshLayoutsPanel();
        }
    }

    private void repositionFreeformDrawer(Point requestedHandle)
    {
        if (config.layoutMode() != TabLayoutMode.FREEFORM
            || !config.drawerEnabled()
            || TabLayout.drawerOrder(config).isEmpty())
        {
            return;
        }

        final Point handle = requestedHandle == null
            ? defaultFreeformDrawerHandle(currentFreeformLocations())
            : clampHandle(requestedHandle);
        final DrawerDirection direction = resolveDrawerDirection(handle, null);
        drawerOverlay.setEffectiveDirection(direction);
        drawerOverlay.setExpanded(drawerExpanded);
        placeDrawerHandle(handle, true);
    }

    private void repositionFreeformDrawerPreview(Point requestedHandle)
    {
        if (config.layoutMode() != TabLayoutMode.FREEFORM
            || !config.drawerEnabled()
            || TabLayout.drawerOrder(config).isEmpty())
        {
            return;
        }

        final Point handle = clampHandle(requestedHandle);
        final DrawerDirection direction = resolveDrawerDirection(handle, null);
        drawerOverlay.setEffectiveDirection(direction);
        drawerOverlay.setExpanded(drawerExpanded);
        placeDrawerHandle(handle, false);
    }

    private void positionDrawerForDock()
    {
        if (config.layoutMode() != TabLayoutMode.DOCKED
            || !config.drawerEnabled()
            || TabLayout.drawerOrder(config).isEmpty())
        {
            return;
        }

        final Rectangle dock = dockOverlay.getBounds();
        if (dock == null || dock.isEmpty())
        {
            return;
        }

        final DrawerDirection direction = resolveDrawerDirection(null, dock);
        drawerOverlay.setEffectiveDirection(direction);
        drawerOverlay.setExpanded(drawerExpanded);

        final int cell = DockMetrics.buttonCellSize(config);
        final int gap = DockMetrics.gap(config);
        final Point handle;
        switch (direction)
        {
            case RIGHT:
                handle = new Point(dock.x + dock.width + gap, dock.y);
                break;
            case UP:
                handle = new Point(dock.x, dock.y - cell - gap);
                break;
            case DOWN:
                handle = new Point(dock.x, dock.y + dock.height + gap);
                break;
            case LEFT:
            default:
                handle = new Point(dock.x - cell - gap, dock.y);
                break;
        }

        placeDrawerHandle(clampHandle(handle), false);
    }

    private DrawerDirection resolveDrawerDirection(Point handle, Rectangle dock)
    {
        final DrawerDirection requested = config.drawerDirection();
        final Dimension canvas = client.getRealDimensions();
        final int cell = DockMetrics.buttonCellSize(config);
        final int count = TabLayout.drawerOrder(config).size();
        final int needed = count * (cell + DockMetrics.gap(config));

        final int left;
        final int right;
        final int up;
        final int down;

        if (dock != null)
        {
            left = Math.max(0, dock.x - cell);
            right = Math.max(0, canvas.width - (dock.x + dock.width) - cell);
            up = Math.max(0, dock.y - cell);
            down = Math.max(0, canvas.height - (dock.y + dock.height) - cell);
        }
        else
        {
            final Point h = handle == null ? defaultFreeformDrawerHandle(currentFreeformLocations()) : handle;
            left = Math.max(0, h.x);
            right = Math.max(0, canvas.width - (h.x + cell));
            up = Math.max(0, h.y);
            down = Math.max(0, canvas.height - (h.y + cell));
        }

        if (requested != DrawerDirection.AUTO)
        {
            if (spaceFor(requested, left, right, up, down) >= needed)
            {
                return requested;
            }
            final DrawerDirection opposite = opposite(requested);
            if (spaceFor(opposite, left, right, up, down) >= needed)
            {
                return opposite;
            }
            return maxSpaceDirection(left, right, up, down);
        }

        if (dock != null)
        {
            final boolean vertical = dock.height >= dock.width;
            if (vertical)
            {
                if (Math.max(left, right) >= needed)
                {
                    return left >= right ? DrawerDirection.LEFT : DrawerDirection.RIGHT;
                }
            }
            else if (Math.max(up, down) >= needed)
            {
                return up >= down ? DrawerDirection.UP : DrawerDirection.DOWN;
            }
        }

        return maxSpaceDirection(left, right, up, down);
    }

    private static int spaceFor(DrawerDirection direction, int left, int right, int up, int down)
    {
        switch (direction)
        {
            case RIGHT: return right;
            case UP: return up;
            case DOWN: return down;
            case LEFT:
            default: return left;
        }
    }

    private static DrawerDirection opposite(DrawerDirection direction)
    {
        switch (direction)
        {
            case LEFT: return DrawerDirection.RIGHT;
            case RIGHT: return DrawerDirection.LEFT;
            case UP: return DrawerDirection.DOWN;
            case DOWN: return DrawerDirection.UP;
            default: return DrawerDirection.LEFT;
        }
    }

    private static DrawerDirection maxSpaceDirection(int left, int right, int up, int down)
    {
        int best = left;
        DrawerDirection direction = DrawerDirection.LEFT;
        if (right > best) { best = right; direction = DrawerDirection.RIGHT; }
        if (up > best) { best = up; direction = DrawerDirection.UP; }
        if (down > best) { direction = DrawerDirection.DOWN; }
        return direction;
    }

    private void placeDrawerHandle(Point handle, boolean persist)
    {
        final Point topLeft = drawerOverlay.topLeftForHandle(handle);
        final Dimension content = drawerOverlay.contentSize();
        final Dimension canvas = client.getRealDimensions();
        final Point clampedTopLeft = new Point(
            clamp(topLeft.x, 0, Math.max(0, canvas.width - content.width)),
            clamp(topLeft.y, 0, Math.max(0, canvas.height - content.height))
        );

        if (persist)
        {
            overlayManager.resetOverlay(drawerOverlay);
            drawerOverlay.setPreferredPosition(null);
            drawerOverlay.setPreferredLocation(clampedTopLeft);
            drawerOverlay.revalidate();
            overlayManager.saveOverlay(drawerOverlay);
        }
        else
        {
            final Point current = drawerOverlay.getPreferredLocation();
            if (current == null || !current.equals(clampedTopLeft))
            {
                drawerOverlay.setPreferredPosition(null);
                drawerOverlay.setPreferredLocation(clampedTopLeft);
                drawerOverlay.revalidate();
            }
        }
    }

    private Point clampHandle(Point handle)
    {
        final Dimension canvas = client.getRealDimensions();
        final int cell = DockMetrics.buttonCellSize(config);
        return new Point(
            clamp(handle.x, 0, Math.max(0, canvas.width - cell)),
            clamp(handle.y, 0, Math.max(0, canvas.height - cell))
        );
    }

    private Point defaultFreeformDrawerHandle()
    {
        return defaultFreeformDrawerHandle(currentFreeformLocations());
    }

    private Point defaultFreeformDrawerHandle(Map<Integer, Point> mainLocations)
    {
        final Dimension canvas = client.getRealDimensions();
        final int cell = DockMetrics.buttonCellSize(config);
        final int gap = DockMetrics.gap(config);

        if (mainLocations != null && !mainLocations.isEmpty())
        {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (Point point : mainLocations.values())
            {
                minX = Math.min(minX, point.x);
                minY = Math.min(minY, point.y);
                maxX = Math.max(maxX, point.x + cell);
                maxY = Math.max(maxY, point.y + cell);
            }

            final Point[] candidates = new Point[]
            {
                new Point(maxX + gap, minY),
                new Point(minX - cell - gap, minY),
                new Point(minX, maxY + gap),
                new Point(minX, minY - cell - gap)
            };
            for (Point candidate : candidates)
            {
                if (candidate.x >= 0
                    && candidate.y >= 0
                    && candidate.x + cell <= canvas.width
                    && candidate.y + cell <= canvas.height)
                {
                    return candidate;
                }
            }
        }

        return new Point(Math.max(0, canvas.width - cell - 12), 12);
    }

    /* ---------- Sidebar-facing activity editor ---------- */

    TabLayoutMode getLayoutMode()
    {
        return config.layoutMode();
    }

    int getButtonsPerRow()
    {
        return config.buttonsPerRow();
    }

    List<Integer> getTabOrder()
    {
        return TabLayout.fullOrder(config);
    }

    TabState getTabState(int tabIndex)
    {
        return TabLayout.state(config, tabIndex);
    }

    boolean isDrawerEnabled()
    {
        return config.drawerEnabled();
    }

    DrawerDirection getDrawerDirection()
    {
        return config.drawerDirection();
    }

    boolean isDrawerCloseAfterSelection()
    {
        return config.drawerCloseAfterSelection();
    }

    boolean isDrawerExpanded()
    {
        return drawerExpanded;
    }

    boolean isStickTogether()
    {
        return config.stickTogether();
    }

    boolean hasActivityPreset(int slot)
    {
        return activityLayoutStore.hasPreset(slot);
    }

    boolean hasPreviousLayout()
    {
        return activityLayoutStore.hasRecovery();
    }

    void setLayoutMode(TabLayoutMode mode)
    {
        if (mode != null && mode != config.layoutMode())
        {
            configManager.setConfiguration(CustomGameTabsConfig.GROUP, "layoutMode", mode.name());
        }
    }

    void setButtonsPerRow(int value)
    {
        final int bounded = clamp(value, 1, LayoutSpec.TABS.length);
        if (bounded != config.buttonsPerRow())
        {
            configManager.setConfiguration(CustomGameTabsConfig.GROUP, "buttonsAcross", bounded);
        }
    }

    void setTabState(int tabIndex, TabState state)
    {
        if (!LayoutSpec.isValidTab(tabIndex) || state == null || state == TabLayout.state(config, tabIndex))
        {
            return;
        }

        clientThread.invoke(() ->
        {
            final Map<Integer, Point> positions = currentFreeformLocations();
            final Point handle = drawerOverlay.getHandleLocation();
            final Set<Integer> drawerTabs = TabLayout.parse(config.drawerTabs());

            applyingActivityLayout = true;
            try
            {
                if (state == TabState.HIDDEN)
                {
                    configManager.setConfiguration(CustomGameTabsConfig.GROUP, TabLayout.showKey(tabIndex), false);
                    drawerTabs.remove(tabIndex);
                }
                else
                {
                    configManager.setConfiguration(CustomGameTabsConfig.GROUP, TabLayout.showKey(tabIndex), true);
                    if (state == TabState.DRAWER)
                    {
                        drawerTabs.add(tabIndex);
                    }
                    else
                    {
                        drawerTabs.remove(tabIndex);
                    }
                }
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, "drawerTabs", TabLayout.encodeTabs(drawerTabs));
            }
            finally
            {
                applyingActivityLayout = false;
            }

            stickyGroups = stickyGroups.retain(TabLayout.mainOrder(config));
            rebuildOverlays(seedMissingMainLocations(positions), handle);
            saveWorkingSnapshot();
            refreshLayoutsPanel();
        });
    }

    void moveTab(int tabIndex, int delta)
    {
        if (!LayoutSpec.isValidTab(tabIndex) || delta == 0)
        {
            return;
        }

        final List<Integer> order = TabLayout.fullOrder(config);
        final int from = order.indexOf(tabIndex);
        final int to = clamp(from + delta, 0, order.size() - 1);
        if (from < 0 || from == to)
        {
            return;
        }
        order.remove(from);
        order.add(to, tabIndex);
        configManager.setConfiguration(
            CustomGameTabsConfig.GROUP,
            "tabOrder",
            TabLayout.canonicalOrderString(order)
        );
    }

    void setDrawerEnabled(boolean enabled)
    {
        if (enabled != config.drawerEnabled())
        {
            configManager.setConfiguration(CustomGameTabsConfig.GROUP, "drawerEnabled", enabled);
        }
    }

    void setDrawerDirection(DrawerDirection direction)
    {
        if (direction != null && direction != config.drawerDirection())
        {
            final Point handle = drawerOverlay.getHandleLocation();
            applyingActivityLayout = true;
            try
            {
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, "drawerDirection", direction.name());
            }
            finally
            {
                applyingActivityLayout = false;
            }

            clientThread.invoke(() ->
            {
                if (config.layoutMode() == TabLayoutMode.FREEFORM)
                {
                    repositionFreeformDrawer(handle);
                }
                else
                {
                    positionDrawerForDock();
                }
                saveWorkingSnapshot();
                refreshLayoutsPanel();
            });
        }
    }

    void setDrawerOpen(boolean open)
    {
        clientThread.invoke(() -> setDrawerExpanded(open, true));
    }

    void setDrawerCloseAfterSelection(boolean close)
    {
        if (close != config.drawerCloseAfterSelection())
        {
            configManager.setConfiguration(CustomGameTabsConfig.GROUP, "drawerCloseAfterSelection", close);
        }
    }

    void setStickTogether(boolean stick)
    {
        if (stick == config.stickTogether())
        {
            return;
        }

        clientThread.invoke(() ->
        {
            applyingActivityLayout = true;
            try
            {
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, "stickTogether", stick);
            }
            finally
            {
                applyingActivityLayout = false;
            }

            stickyGroups = stick
                ? StickyGroups.detect(
                    currentFreeformLocations(),
                    DockMetrics.buttonCellSize(config),
                    DockMetrics.gap(config)
                )
                : new StickyGroups();
            updateSnapAvailability();
            log.info(
                "[CGT 2.2.2] stickTogether={} groups={}",
                stick,
                stickyGroups.asLists()
            );
            saveWorkingSnapshot();
            showLayoutsStatus(stick ? "Stuck snapped tabs together" : "Tabs move independently");
            refreshLayoutsPanel();
        });
    }

    void saveActivityPreset(int slot)
    {
        clientThread.invoke(() ->
        {
            final ActivityLayoutSnapshot snapshot = captureCurrentActivity();
            if (snapshot == null)
            {
                showLayoutsStatus("Nothing to save");
                return;
            }
            activityLayoutStore.savePreset(slot, snapshot);
            activityLayoutStore.saveWorking(snapshot);
            showLayoutsStatus("Saved Layout " + slot);
            refreshLayoutsPanel();
        });
    }

    void loadActivityPreset(int slot)
    {
        clientThread.invoke(() ->
        {
            final ActivityLayoutSnapshot snapshot = activityLayoutStore.loadPreset(slot);
            if (snapshot == null)
            {
                showLayoutsStatus("Layout " + slot + " is empty");
                refreshLayoutsPanel();
                return;
            }

            saveRecoverySnapshot();
            applyActivitySnapshot(snapshot);
            saveWorkingSnapshot();
            showLayoutsStatus("Loaded Layout " + slot);
            refreshLayoutsPanel();
        });
    }

    void resetWorkingLayout()
    {
        clientThread.invoke(() ->
        {
            if (config.layoutMode() != TabLayoutMode.FREEFORM || looseOverlays.isEmpty())
            {
                return;
            }
            saveRecoverySnapshot();
            applyLocations(buildWorkingGrid());
            saveWorkingSnapshot();
            showLayoutsStatus("Working layout reset");
            refreshLayoutsPanel();
        });
    }

    void restorePreviousLayout()
    {
        clientThread.invoke(() ->
        {
            final ActivityLayoutSnapshot snapshot = activityLayoutStore.loadRecovery();
            if (snapshot == null)
            {
                showLayoutsStatus("No previous layout");
                refreshLayoutsPanel();
                return;
            }
            applyActivitySnapshot(snapshot);
            saveWorkingSnapshot();
            showLayoutsStatus("Restored previous layout");
            refreshLayoutsPanel();
        });
    }

    private void applyActivitySnapshot(ActivityLayoutSnapshot snapshot)
    {
        applyActivitySettings(snapshot);
        final Map<Integer, Point> locations = config.layoutMode() == TabLayoutMode.FREEFORM
            ? resolveLocations(snapshot)
            : null;
        final Point handle = config.layoutMode() == TabLayoutMode.FREEFORM
            ? resolveDrawerHandle(snapshot)
            : null;
        rebuildOverlays(locations, handle);
    }

    private void applyActivitySettings(ActivityLayoutSnapshot snapshot)
    {
        applyingActivityLayout = true;
        try
        {
            if (snapshot.isLegacyPlacementOnly())
            {
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, "layoutMode", TabLayoutMode.FREEFORM.name());
                drawerExpanded = false;
                stickyGroups = new StickyGroups();
                return;
            }

            configManager.setConfiguration(CustomGameTabsConfig.GROUP, "layoutMode", snapshot.getLayoutMode().name());
            configManager.setConfiguration(CustomGameTabsConfig.GROUP, "buttonsAcross", snapshot.getButtonsPerRow());
            configManager.setConfiguration(CustomGameTabsConfig.GROUP, "tabOrder", TabLayout.canonicalOrderString(snapshot.getOrder()));

            final Set<Integer> drawer = new LinkedHashSet<>();
            for (int tab = 0; tab < LayoutSpec.TABS.length; tab++)
            {
                final TabState state = snapshot.getState(tab);
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, TabLayout.showKey(tab), state != TabState.HIDDEN);
                if (state == TabState.DRAWER)
                {
                    drawer.add(tab);
                }
            }
            configManager.setConfiguration(CustomGameTabsConfig.GROUP, "drawerTabs", TabLayout.encodeTabs(drawer));
            configManager.setConfiguration(CustomGameTabsConfig.GROUP, "drawerEnabled", snapshot.isDrawerEnabled());
            configManager.setConfiguration(CustomGameTabsConfig.GROUP, "drawerDirection", snapshot.getDrawerDirection().name());
            configManager.setConfiguration(CustomGameTabsConfig.GROUP, "drawerCloseAfterSelection", snapshot.isDrawerCloseAfterSelection());
            configManager.setConfiguration(CustomGameTabsConfig.GROUP, "stickTogether", snapshot.isStickTogether());

            if (snapshot.hasAppearance())
            {
                final ActivityLayoutSnapshot.Appearance appearance = snapshot.getAppearance();
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, "buttonScale", appearance.getButtonScale());
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, "gap", appearance.getGap());
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, "idleOpacity", appearance.getIdleOpacity());
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, "hoverOpacity", appearance.getHoverOpacity());
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, "selectedOpacity", appearance.getSelectedOpacity());
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, "frameOpacity", appearance.getFrameOpacity());
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, "showTooltip", appearance.isShowTooltip());
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, "buttonColor", appearance.getButtonColor());
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, "hoverColor", appearance.getHoverColor());
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, "selectedColor", appearance.getSelectedColor());
                configManager.setConfiguration(CustomGameTabsConfig.GROUP, "borderColor", appearance.getBorderColor());
            }

            drawerExpanded = snapshot.isDrawerExpanded();
            stickyGroups = snapshot.isStickTogether()
                ? snapshot.getStickyGroups().retain(TabLayout.mainOrder(config))
                : new StickyGroups();
        }
        finally
        {
            applyingActivityLayout = false;
        }
    }

    private ActivityLayoutSnapshot captureCurrentActivity()
    {
        final Dimension dimensions = client.getRealDimensions();
        final TabState[] states = new TabState[LayoutSpec.TABS.length];
        for (int tab = 0; tab < states.length; tab++)
        {
            states[tab] = TabLayout.state(config, tab);
        }

        return new ActivityLayoutSnapshot(
            dimensions.width,
            dimensions.height,
            config.layoutMode(),
            config.buttonsPerRow(),
            TabLayout.fullOrder(config),
            states,
            config.drawerEnabled(),
            config.drawerDirection(),
            config.drawerCloseAfterSelection(),
            drawerExpanded,
            drawerOverlay.getHandleLocation(),
            config.stickTogether(),
            stickyGroups,
            currentFreeformLocations(),
            new ActivityLayoutSnapshot.Appearance(
                config.buttonScale(),
                config.gap(),
                config.idleOpacity(),
                config.hoverOpacity(),
                config.selectedOpacity(),
                config.frameOpacity(),
                config.showTooltip(),
                config.buttonColor(),
                config.hoverColor(),
                config.selectedColor(),
                config.borderColor()
            )
        );
    }

    private void saveRecoverySnapshot()
    {
        final ActivityLayoutSnapshot snapshot = captureCurrentActivity();
        if (snapshot != null)
        {
            activityLayoutStore.saveRecovery(snapshot);
        }
    }

    private void saveWorkingSnapshot()
    {
        final ActivityLayoutSnapshot snapshot = captureCurrentActivity();
        if (snapshot != null)
        {
            activityLayoutStore.saveWorking(snapshot);
        }
    }

    private void handleCanvasResize()
    {
        final Dimension dimensions = client.getRealDimensions();
        final int width = Math.max(1, dimensions.width);
        final int height = Math.max(1, dimensions.height);

        if (lastCanvasWidth <= 0 || lastCanvasHeight <= 0)
        {
            lastCanvasWidth = width;
            lastCanvasHeight = height;
            return;
        }

        if (width == lastCanvasWidth && height == lastCanvasHeight)
        {
            return;
        }

        final int previousWidth = lastCanvasWidth;
        final int previousHeight = lastCanvasHeight;

        /* If a resize somehow happens during an owned Alt-drag, leave the old
         * dimensions pending. The first tick after release will then perform
         * the resize reflow instead of silently accepting transient geometry. */
        if (altDragGesture)
        {
            return;
        }

        lastCanvasWidth = width;
        lastCanvasHeight = height;

        if (client.getGameState() != GameState.LOGGED_IN
            || config.layoutMode() != TabLayoutMode.FREEFORM)
        {
            return;
        }

        final ActivityLayoutSnapshot working = activityLayoutStore.loadWorking();
        if (working == null)
        {
            return;
        }

        final Map<Integer, Point> resolved = working.resolveLocations(
            width,
            height,
            DockMetrics.buttonCellSize(config)
        );
        applyLocations(resolved);

        if (config.drawerEnabled() && !TabLayout.drawerOrder(config).isEmpty())
        {
            final Point handle = resolveDrawerHandle(working);
            if (handle != null)
            {
                repositionFreeformDrawer(handle);
            }
        }

        /* Keep membership from the canonical snapshot. Its geometry is
         * translated rigidly, so resizing must never dissolve a group. */
        stickyGroups = working.isStickTogether()
            ? working.getStickyGroups().retain(TabLayout.mainOrder(config))
            : new StickyGroups();

        log.info(
            "[CGT 2.2.2] canvas resize {}x{} -> {}x{} groups={} positions={}",
            previousWidth,
            previousHeight,
            width,
            height,
            stickyGroups.asLists(),
            resolved
        );
    }

    private Map<Integer, Point> resolveLocations(ActivityLayoutSnapshot snapshot)
    {
        final Dimension dimensions = client.getRealDimensions();
        return snapshot.resolveLocations(
            dimensions.width,
            dimensions.height,
            DockMetrics.buttonCellSize(config)
        );
    }

    private Point resolveDrawerHandle(ActivityLayoutSnapshot snapshot)
    {
        final Dimension dimensions = client.getRealDimensions();
        final int cell = DockMetrics.buttonCellSize(config);
        final Point raw = snapshot.getDrawerHandle();

        /* 2.2.0's original Freeform default was the isolated top-right corner.
         * Migrate only that exact legacy default; intentionally moved handles
         * remain exactly where the user saved them. */
        if (raw != null
            && Math.abs(raw.x - Math.max(0, snapshot.getCanvasWidth() - cell - 12)) <= 4
            && Math.abs(raw.y - 12) <= 4)
        {
            final Point migrated = defaultFreeformDrawerHandle(resolveLocations(snapshot));
            log.info(
                "[CGT 2.2.2] migrated legacy drawer handle {} -> {}",
                raw,
                migrated
            );
            return migrated;
        }

        return snapshot.resolveDrawerHandle(
            dimensions.width,
            dimensions.height,
            cell
        );
    }

    private void applyLocations(Map<Integer, Point> locations)
    {
        if (locations == null || locations.isEmpty())
        {
            return;
        }
        for (CustomGameTabOverlay overlay : looseOverlays)
        {
            final Point location = locations.get(overlay.getTabIndex());
            if (location != null)
            {
                /* Resolved layouts are already clamped as whole rigid
                 * components. Never clamp each tab independently here: that
                 * is precisely what collapses a right-edge sticky group into
                 * a stack when the canvas narrows. */
                placeOverlay(overlay, location);
            }
        }
    }

    private void placeOverlay(CustomGameTabOverlay overlay, Point location)
    {
        overlayManager.resetOverlay(overlay);
        overlay.setPreferredPosition(null);
        overlay.setPreferredLocation(new Point(location));
        overlay.revalidate();
        overlayManager.saveOverlay(overlay);
    }

    private Point clampTabLocation(Point location)
    {
        final Dimension dimensions = client.getRealDimensions();
        final int cell = DockMetrics.buttonCellSize(config);
        return new Point(
            clamp(location.x, 0, Math.max(0, dimensions.width - cell)),
            clamp(location.y, 0, Math.max(0, dimensions.height - cell))
        );
    }

    private Map<Integer, Point> currentFreeformLocations()
    {
        final Map<Integer, Point> current = new LinkedHashMap<>();
        for (CustomGameTabOverlay overlay : looseOverlays)
        {
            final Rectangle bounds = overlay.getBounds();
            if (bounds != null && !bounds.isEmpty())
            {
                current.put(overlay.getTabIndex(), bounds.getLocation());
            }
            else if (overlay.getPreferredLocation() != null)
            {
                current.put(overlay.getTabIndex(), new Point(overlay.getPreferredLocation()));
            }
        }
        return current;
    }

    private Map<Integer, Point> seedMissingMainLocations(Map<Integer, Point> existing)
    {
        final Map<Integer, Point> result = copyPoints(existing);
        final List<Integer> main = TabLayout.mainOrder(config);
        if (main.isEmpty())
        {
            return result;
        }

        int anchorX = Integer.MAX_VALUE;
        int anchorY = Integer.MAX_VALUE;
        for (Point point : result.values())
        {
            anchorX = Math.min(anchorX, point.x);
            anchorY = Math.min(anchorY, point.y);
        }
        if (anchorX == Integer.MAX_VALUE)
        {
            anchorX = Math.max(0, client.getRealDimensions().width - DockMetrics.buttonCellSize(config) - 12);
            anchorY = 12;
        }

        final int cell = DockMetrics.buttonCellSize(config);
        final int gap = DockMetrics.gap(config);
        final int columns = Math.max(1, Math.min(config.buttonsPerRow(), main.size()));
        for (int i = 0; i < main.size(); i++)
        {
            final int tab = main.get(i);
            if (!result.containsKey(tab))
            {
                result.put(
                    tab,
                    clampTabLocation(new Point(
                        anchorX + (i % columns) * (cell + gap),
                        anchorY + (i / columns) * (cell + gap)
                    ))
                );
            }
        }
        return result;
    }

    private Map<Integer, Point> buildWorkingGrid()
    {
        final Map<Integer, Point> current = currentFreeformLocations();
        final List<Integer> order = TabLayout.mainOrder(config);
        final Map<Integer, Point> result = new LinkedHashMap<>();
        if (order.isEmpty())
        {
            return result;
        }

        int anchorX = Integer.MAX_VALUE;
        int anchorY = Integer.MAX_VALUE;
        for (Point point : current.values())
        {
            anchorX = Math.min(anchorX, point.x);
            anchorY = Math.min(anchorY, point.y);
        }
        if (anchorX == Integer.MAX_VALUE)
        {
            anchorX = 0;
            anchorY = 0;
        }

        final int cell = DockMetrics.buttonCellSize(config);
        final int gap = DockMetrics.gap(config);
        final int columns = Math.max(1, Math.min(config.buttonsPerRow(), order.size()));
        final int rows = (order.size() + columns - 1) / columns;
        final int gridWidth = columns * cell + Math.max(0, columns - 1) * gap;
        final int gridHeight = rows * cell + Math.max(0, rows - 1) * gap;
        final Dimension dimensions = client.getRealDimensions();
        anchorX = clamp(anchorX, 0, Math.max(0, dimensions.width - gridWidth));
        anchorY = clamp(anchorY, 0, Math.max(0, dimensions.height - gridHeight));

        for (int i = 0; i < order.size(); i++)
        {
            result.put(
                order.get(i),
                new Point(
                    anchorX + (i % columns) * (cell + gap),
                    anchorY + (i / columns) * (cell + gap)
                )
            );
        }
        return result;
    }

    private boolean hasRenderableLayout()
    {
        if (config.layoutMode() == TabLayoutMode.DOCKED)
        {
            return dockOverlay.getBounds() != null && !dockOverlay.getBounds().isEmpty();
        }
        if (looseOverlays.isEmpty())
        {
            return TabLayout.mainOrder(config).isEmpty();
        }
        for (CustomGameTabOverlay overlay : looseOverlays)
        {
            if (overlay.getBounds() == null || overlay.getBounds().isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    private static Map<Integer, Point> copyPoints(Map<Integer, Point> source)
    {
        final Map<Integer, Point> copy = new LinkedHashMap<>();
        if (source != null)
        {
            for (Map.Entry<Integer, Point> entry : source.entrySet())
            {
                if (entry.getValue() != null)
                {
                    copy.put(entry.getKey(), new Point(entry.getValue()));
                }
            }
        }
        return copy;
    }

    private static int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(max, value));
    }

    private void addLayoutsNavigation()
    {
        if (layoutsNavigation != null)
        {
            return;
        }

        layoutsPanel = new TabLayoutsPanel(this);
        final BufferedImage icon = TabLayoutsPanel.createNavigationIcon();
        layoutsNavigation = NavigationButton.builder()
            .tooltip("Tab Layouts")
            .icon(icon)
            .priority(6)
            .panel(layoutsPanel)
            .build();
        clientToolbar.addNavigation(layoutsNavigation);
        refreshLayoutsPanel();
    }

    private void removeLayoutsNavigation()
    {
        if (layoutsNavigation != null)
        {
            clientToolbar.removeNavigation(layoutsNavigation);
        }
        layoutsNavigation = null;
        layoutsPanel = null;
    }

    private void refreshLayoutsPanel()
    {
        final TabLayoutsPanel panel = layoutsPanel;
        if (panel != null)
        {
            panel.refresh();
        }
    }

    private void showLayoutsStatus(String text)
    {
        final TabLayoutsPanel panel = layoutsPanel;
        if (panel != null)
        {
            panel.showStatus(text);
        }
    }
    private void migrateLegacyConfiguration()
    {
        migrateColumns();
        migrateHiddenButtons();
        migrateLayoutMode();
        migrateButtonScale();
        migrateDefaultColors();
        removeObsoleteInterfaceSettings();
    }

    private void removeObsoleteInterfaceSettings()
    {
        final String[] keys =
        {
            "scaleSidePanel",
            "sidePanelScale",
            "useGlobalUiScale",
            "globalUiMagnification",
            "keepSidePanelOpen",
            "restoreLastSidePanel",
            "lastSidePanelTab",
            "hideVanillaRail",
            "snapLinks",
            "columns",
            "hiddenTabs",
            "layoutStyle",
            "buttonSize"
        };

        for (String key : keys)
        {
            configManager.unsetConfiguration(
                CustomGameTabsConfig.GROUP,
                key
            );
        }
    }

    private void migrateColumns()
    {
        if (
            configManager.getConfiguration(
                CustomGameTabsConfig.GROUP,
                "buttonsAcross"
            ) != null
        )
        {
            return;
        }

        final String oldColumns =
            configManager.getConfiguration(
                CustomGameTabsConfig.GROUP,
                "columns"
            );

        if ("ONE".equalsIgnoreCase(oldColumns))
        {
            configManager.setConfiguration(
                CustomGameTabsConfig.GROUP,
                "buttonsAcross",
                1
            );
        }
        else if ("TWO".equalsIgnoreCase(oldColumns))
        {
            configManager.setConfiguration(
                CustomGameTabsConfig.GROUP,
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
                CustomGameTabsConfig.GROUP,
                "buttonsAcross",
                oldColumns
            );
        }
    }

    private void migrateHiddenButtons()
    {
        final String hidden =
            configManager.getConfiguration(
                CustomGameTabsConfig.GROUP,
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
                    CustomGameTabsConfig.GROUP,
                    key
                ) == null
            )
            {
                configManager.setConfiguration(
                    CustomGameTabsConfig.GROUP,
                    key,
                    false
                );
            }
        }
    }

    private void migrateLayoutMode()
    {
        final String existingMode =
            configManager.getConfiguration(
                CustomGameTabsConfig.GROUP,
                "layoutMode"
            );

        if (existingMode == null)
        {
            final String separate =
                configManager.getConfiguration(
                    CustomGameTabsConfig.GROUP,
                    "moveSeparately"
                );
            final String oldMode =
                configManager.getConfiguration(
                    CustomGameTabsConfig.GROUP,
                    "layoutStyle"
                );

            final boolean freeform =
                "true".equalsIgnoreCase(separate)
                    || "MOVE_ONE_BY_ONE".equalsIgnoreCase(oldMode);

            configManager.setConfiguration(
                CustomGameTabsConfig.GROUP,
                "layoutMode",
                freeform
                    ? TabLayoutMode.FREEFORM.name()
                    : TabLayoutMode.DOCKED.name()
            );
        }

        configManager.unsetConfiguration(
            CustomGameTabsConfig.GROUP,
            "moveSeparately"
        );
    }

    private void migrateButtonScale()
    {
        final String raw =
            configManager.getConfiguration(
                CustomGameTabsConfig.GROUP,
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
                    CustomGameTabsConfig.GROUP,
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
                CustomGameTabsConfig.GROUP,
                "buttonScale",
                value
            );
            return;
        }

        final String oldSize =
            configManager.getConfiguration(
                CustomGameTabsConfig.GROUP,
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
            CustomGameTabsConfig.GROUP,
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
            new java.awt.Color(75, 64, 45, 240),
            new java.awt.Color(82, 62, 36, 246)
        );
        migrateDefaultColor(
            "selectedColor",
            new java.awt.Color(97, 76, 39, 245),
            new java.awt.Color(112, 82, 35, 250)
        );
        migrateDefaultColor(
            "borderColor",
            new java.awt.Color(126, 100, 54, 240),
            new java.awt.Color(151, 113, 49, 248)
        );
    }

    private void migrateDefaultColor(
        String key,
        java.awt.Color oldDefault,
        java.awt.Color newDefault
    )
    {
        final java.awt.Color current =
            configManager.getConfiguration(
                CustomGameTabsConfig.GROUP,
                key,
                java.awt.Color.class
            );

        if (current != null && current.equals(oldDefault))
        {
            configManager.setConfiguration(
                CustomGameTabsConfig.GROUP,
                key,
                newDefault
            );
        }
    }
}
