package com.freddy.customgametabs;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.Queue;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.PostClientTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.stretchedmode.TranslateMouseListener;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Exercises RuneLite's real mouse pipeline and translator; no game input is generated. */
public class InputPipelineTest
{
    @Rule public MockitoRule mocks = MockitoJUnit.rule();
    @Mock private Client client;
    @Mock private ClientThread clientThread;
    @Mock private OverlayManager overlayManager;
    @Mock private ClientToolbar clientToolbar;
    @Mock private ConfigManager configManager;
    @Mock private TabIconRenderer iconRenderer;
    @Mock private TabSnapManager snapManager;
    @Mock private TabTooltipPresenter tooltipPresenter;
    @Mock private ActivityLayoutStore activityLayoutStore;
    @Mock private NativeTabController nativeTabController;
    @Mock private NativeTabPresentation nativeTabPresentation;
    @Mock private NativeTabMenuBridge nativeTabMenuBridge;
    @Mock private CustomGameTabsOverlay dockOverlay;
    @Mock private DrawerOverlay drawerOverlay;
    @Spy private CustomGameTabsConfig config = new CustomGameTabsConfig() {};
    @Spy private MouseManager mouseManager = Guice.createInjector(new AbstractModule()
    {
        @Override protected void configure()
        {
            bind(RuneLiteConfig.class).toInstance(new RuneLiteConfig() {});
        }
    }).getInstance(MouseManager.class);
    @InjectMocks private CustomGameTabsPlugin plugin;

    private final Queue<Runnable> pending = new ArrayDeque<>();

    @Before
    public void setup()
    {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getTopLevelInterfaceId()).thenReturn(InterfaceID.TOPLEVEL_PRE_EOC);
        when(client.getRealDimensions()).thenReturn(new Dimension(800, 600));
        when(client.getStretchedDimensions()).thenReturn(new Dimension(1200, 900));
        when(drawerOverlay.tabAt(anyInt(), anyInt())).thenReturn(-1);
        when(dockOverlay.getBounds()).thenReturn(new Rectangle(400, 300, 42, 42));
        when(dockOverlay.containsInputSurface(anyInt(), anyInt())).thenAnswer(call ->
            new Rectangle(400, 300, 42, 42).contains((int) call.getArgument(0), (int) call.getArgument(1)));
        when(dockOverlay.tabAt(anyInt(), anyInt())).thenAnswer(call ->
            new Rectangle(400, 300, 42, 42).contains((int) call.getArgument(0), (int) call.getArgument(1)) ? 3 : -1);
        doAnswer(call -> { ((Runnable) call.getArgument(0)).run(); return null; })
            .when(clientThread).invoke(any(Runnable.class));
        doAnswer(call -> { pending.add(call.getArgument(0)); return null; })
            .when(clientThread).invokeLater(any(Runnable.class));
    }

    @Test
    public void rapidRestartDiscardsInitializationFromPreviousLifecycle()
    {
        doAnswer(call -> { pending.add(call.getArgument(0)); return null; })
            .when(clientThread).invoke(any(Runnable.class));
        plugin.startUp();
        plugin.shutDown();
        plugin.startUp();
        while (!pending.isEmpty())
        {
            pending.remove().run();
        }
        verify(nativeTabController, times(1)).startUp();
        plugin.shutDown();
        while (!pending.isEmpty())
        {
            pending.remove().run();
        }
    }

    @Test
    public void stretchedEnabledBeforePluginAndToggledAfterwardUsesOneTransform()
    {
        TranslateMouseListener translator = new TranslateMouseListener(client);
        mouseManager.registerMouseListener(0, translator);
        start();
        assertClick(630, 480, true);
        assertClick(420, 320, false); // formerly the invisible duplicate hitbox
        mouseManager.unregisterMouseListener(translator);
        assertClick(420, 320, true);
        mouseManager.registerMouseListener(0, translator);
        assertClick(630, 480, true);
        plugin.shutDown();
    }

    @Test
    public void stretchedEnabledAfterPluginAlsoUsesOneTransform()
    {
        start();
        mouseManager.registerMouseListener(0, new TranslateMouseListener(client));
        assertClick(630, 480, true);
        plugin.shutDown();
    }

    @Test
    public void queuedActionIsCancelledByShutdownAndRegionsExpireEveryFrame()
    {
        start();
        MouseEvent event = press(420, 320);
        assertTrue(mouseManager.processMousePressed(event).isConsumed());
        plugin.onBeforeRender(new BeforeRender());
        verify(dockOverlay, atLeastOnce()).invalidateHitRegions();
        plugin.shutDown();
        drain();
        verify(nativeTabController, never()).activateTab(anyInt());
    }

    @Test
    public void unsupportedRootDoesNotOwnOldScreenCoordinates()
    {
        start();
        when(client.getTopLevelInterfaceId()).thenReturn(-1);
        plugin.onPostClientTick(new PostClientTick());
        assertClick(420, 320, false);
        plugin.shutDown();
    }

    @Test
    public void nonUniformAndIntegerStretchingUseRuneLitesCurrentDimensions()
    {
        start();
        mouseManager.registerMouseListener(0, new TranslateMouseListener(client));
        when(client.getStretchedDimensions()).thenReturn(new Dimension(1200, 720));
        assertClick(630, 384, true);
        when(client.getStretchedDimensions()).thenReturn(new Dimension(1600, 1200));
        assertClick(840, 640, true);
        plugin.shutDown();
    }

    @Test
    public void stretchedStickyDragMovesOnceOnClientThreadAndSavesFinalPositions()
    {
        doReturn(TabLayoutMode.FREEFORM).when(config).layoutMode();
        doReturn(true).when(config).stickTogether();
        start();
        org.mockito.ArgumentCaptor<net.runelite.client.ui.overlay.Overlay> added =
            org.mockito.ArgumentCaptor.forClass(net.runelite.client.ui.overlay.Overlay.class);
        verify(overlayManager, atLeastOnce()).add(added.capture());
        java.util.List<CustomGameTabOverlay> tabs = new java.util.ArrayList<>();
        java.awt.Graphics2D graphics = new java.awt.image.BufferedImage(800, 600,
            java.awt.image.BufferedImage.TYPE_INT_ARGB).createGraphics();
        try
        {
            for (net.runelite.client.ui.overlay.Overlay overlay : added.getAllValues())
            {
                if (overlay instanceof CustomGameTabOverlay)
                {
                    CustomGameTabOverlay tab = (CustomGameTabOverlay) overlay;
                    tab.getBounds().setBounds(100 + tab.getTabIndex() * 44, 100, 42, 42);
                    tab.render(graphics);
                    assertFalse(tab.isMovable());
                    tabs.add(tab);
                }
            }
        }
        finally { graphics.dispose(); }
        mouseManager.registerMouseListener(0, new TranslateMouseListener(client));
        int modifiers = java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
        assertTrue(mouseManager.processMousePressed(new MouseEvent(new Canvas(), MouseEvent.MOUSE_PRESSED,
            0, modifiers, 165, 165, 1, false, MouseEvent.BUTTON1)).isConsumed());
        drain();
        assertTrue(mouseManager.processMouseDragged(new MouseEvent(new Canvas(), MouseEvent.MOUSE_DRAGGED,
            0, modifiers, 225, 225, 0, false, MouseEvent.NOBUTTON)).isConsumed());
        assertEquals(new java.awt.Point(100, 100), tabs.get(0).getPreferredLocation());
        drain();
        assertEquals(new java.awt.Point(140, 140), tabs.get(0).getPreferredLocation());
        assertTrue(mouseManager.processMouseReleased(new MouseEvent(new Canvas(), MouseEvent.MOUSE_RELEASED,
            0, java.awt.event.InputEvent.ALT_DOWN_MASK, 225, 225, 1, false, MouseEvent.BUTTON1)).isConsumed());
        drain();
        for (CustomGameTabOverlay tab : tabs)
        {
            assertEquals(new java.awt.Point(140 + tab.getTabIndex() * 44, 140), tab.getPreferredLocation());
        }
        org.mockito.ArgumentCaptor<ActivityLayoutSnapshot> saved =
            org.mockito.ArgumentCaptor.forClass(ActivityLayoutSnapshot.class);
        verify(activityLayoutStore, atLeastOnce()).saveWorking(saved.capture());
        assertEquals(new java.awt.Point(140, 140), saved.getValue().resolveLocations(800, 600, 42).get(0));
        verify(nativeTabController, never()).activateTab(anyInt());
        plugin.shutDown();
    }

    private void start()
    {
        plugin.startUp();
        plugin.onPostClientTick(new PostClientTick());
    }

    private void assertClick(int x, int y, boolean expected)
    {
        clearInvocations(nativeTabController);
        assertEquals(expected, mouseManager.processMousePressed(press(x, y)).isConsumed());
        drain();
        verify(nativeTabController, times(expected ? 1 : 0)).activateTab(3);
        mouseManager.processMouseReleased(new MouseEvent(new Canvas(), MouseEvent.MOUSE_RELEASED,
            0, 0, x, y, 1, false, MouseEvent.BUTTON1));
        mouseManager.processMouseClicked(new MouseEvent(new Canvas(), MouseEvent.MOUSE_CLICKED,
            0, 0, x, y, 1, false, MouseEvent.BUTTON1));
        drain();
    }

    private static MouseEvent press(int x, int y)
    {
        return new MouseEvent(new Canvas(), MouseEvent.MOUSE_PRESSED, 0, 0, x, y, 1, false, MouseEvent.BUTTON1);
    }

    private void drain()
    {
        while (!pending.isEmpty()) pending.remove().run();
    }
}
