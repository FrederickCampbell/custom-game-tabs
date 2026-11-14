package com.freddy.customgametabs;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.VarClientID;
import net.runelite.client.plugins.mousehighlight.MouseHighlightConfig;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TabTooltipPresenterTest
{
    @Test
    @SuppressWarnings("unchecked")
    public void realMouseHighlightRunsBeforeFallbackAndProducesOnlyOneTooltip() throws Exception
    {
        Client client = mock(Client.class);
        TooltipManager manager = new TooltipManager();
        MenuEntry entry = mock(MenuEntry.class);
        when(entry.getType()).thenReturn(MenuAction.CC_OP);
        when(entry.getOption()).thenReturn("Magic");
        when(entry.getTarget()).thenReturn("");
        when(client.getMenuEntries()).thenReturn(new MenuEntry[] {entry});
        Constructor<?> constructor = Class.forName("net.runelite.client.plugins.mousehighlight.MouseHighlightOverlay")
            .getDeclaredConstructor(Client.class, TooltipManager.class, MouseHighlightConfig.class);
        constructor.setAccessible(true);
        MouseHighlightConfig config = mock(MouseHighlightConfig.class);
        when(config.uiTooltip()).thenReturn(true);
        when(config.chatboxTooltip()).thenReturn(true);
        Overlay mouseHighlight = (Overlay) constructor.newInstance(client, manager, config);
        TabTooltipPresenter fallback = new TabTooltipPresenter(client, manager);
        fallback.setTextSupplier(() -> "Magic");
        Field field = OverlayManager.class.getDeclaredField("OVERLAY_COMPARATOR");
        field.setAccessible(true);
        Comparator<Overlay> comparator = (Comparator<Overlay>) field.get(null);
        Graphics2D graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try
        {
            for (List<Overlay> overlays : Arrays.asList(
                new ArrayList<>(Arrays.asList(fallback, mouseHighlight)),
                new ArrayList<>(Arrays.asList(mouseHighlight, fallback))))
            {
                manager.clear();
                overlays.sort(comparator);
                assertSame(mouseHighlight, overlays.get(0));
                for (Overlay overlay : overlays) overlay.render(graphics);
                assertEquals(1, manager.getTooltips().size());
                assertEquals("Magic", manager.getTooltips().get(0).getText());
            }
            manager.clear();
            when(config.uiTooltip()).thenReturn(false);
            mouseHighlight.render(graphics);
            fallback.render(graphics);
            assertEquals(1, manager.getTooltips().size());
            manager.clear();
            when(client.getVarcIntValue(VarClientID.TOOLTIP_BUILT)).thenReturn(1);
            fallback.render(graphics);
            assertTrue(manager.getTooltips().isEmpty());
            when(client.getVarcIntValue(VarClientID.TOOLTIP_BUILT)).thenReturn(0);
            fallback.setTextSupplier(() -> null);
            fallback.render(graphics);
            assertTrue(manager.getTooltips().isEmpty());
        }
        finally { graphics.dispose(); }
    }
}
