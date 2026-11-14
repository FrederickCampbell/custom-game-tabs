package com.freddy.customgametabs;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OverlayHitLifecycleTest
{
    @Test
    public void looseTabCannotBeDraggedTwiceAndOnlyHitsItsPublishedRectangle()
    {
        Client client = loggedInClient();
        CustomGameTabsConfig config = new CustomGameTabsConfig()
        {
            @Override public TabLayoutMode layoutMode() { return TabLayoutMode.FREEFORM; }
        };
        CustomGameTabOverlay overlay = new CustomGameTabOverlay(client, config,
            mock(TabIconRenderer.class), mock(NativeTabController.class), mock(TabTooltipPresenter.class), 3);
        assertFalse(overlay.isMovable());
        overlay.getBounds().setLocation(100, 100);
        Graphics2D graphics = graphics();
        try
        {
            overlay.render(graphics);
            assertTrue(overlay.contains(110, 110));
            overlay.getBounds().setLocation(400, 400);
            assertFalse(overlay.contains(410, 410));
            overlay.invalidateHitRegions();
            assertFalse(overlay.contains(110, 110));
            overlay.render(graphics);
            assertTrue(overlay.contains(410, 410));
            when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
            overlay.render(graphics);
            assertFalse(overlay.contains(410, 410));
        }
        finally { graphics.dispose(); }
    }

    @Test
    public void dockAndDrawerInvalidateWithoutNeedingAnotherRender()
    {
        Client client = loggedInClient();
        CustomGameTabsConfig config = new CustomGameTabsConfig()
        {
            @Override public String drawerTabs() { return "3"; }
        };
        CustomGameTabsOverlay dock = new CustomGameTabsOverlay(client, config,
            mock(TabIconRenderer.class), mock(NativeTabController.class), mock(TabTooltipPresenter.class));
        DrawerOverlay drawer = new DrawerOverlay(client, config,
            mock(TabIconRenderer.class), mock(NativeTabController.class), mock(TabTooltipPresenter.class));
        dock.getBounds().setLocation(100, 100);
        drawer.getBounds().setLocation(300, 100);
        Graphics2D graphics = graphics();
        try
        {
            dock.render(graphics);
            drawer.render(graphics);
            assertEquals(0, dock.tabAt(110, 110));
            assertTrue(drawer.handleContains(350, 110));
            assertFalse(drawer.containsInputSurface(310, 110)); // closed drawer's transparent extent
            dock.invalidateHitRegions();
            drawer.invalidateHitRegions();
            assertEquals(-1, dock.tabAt(110, 110));
            assertFalse(drawer.handleContains(350, 110));
        }
        finally { graphics.dispose(); }
    }

    private static Graphics2D graphics()
    {
        return new BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB).createGraphics();
    }

    private static Client loggedInClient()
    {
        Client client = mock(Client.class);
        when(client.getRealDimensions()).thenReturn(new java.awt.Dimension(800, 800));
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getTopLevelInterfaceId()).thenReturn(InterfaceID.TOPLEVEL_PRE_EOC);
        return client;
    }
}
