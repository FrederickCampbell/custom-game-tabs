package com.freddy.customgametabs;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import net.runelite.api.Client;
import net.runelite.api.SpritePixels;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.SpriteManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TabIconRendererTest
{
    @Test
    public void widgetOverridesWinAndReplacementAndRemovalInvalidateImmediately()
    {
        Client client = mock(Client.class);
        when(client.getSpriteOverrides()).thenReturn(new HashMap<>());
        when(client.getWidgetSpriteOverrides()).thenReturn(new HashMap<>());
        SpriteManager manager = mock(SpriteManager.class);
        Widget widget = mock(Widget.class);
        when(widget.getId()).thenReturn(100);
        when(widget.getSpriteId()).thenReturn(200);
        BufferedImage original = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        when(manager.getSprite(200, 0)).thenReturn(original);
        SpritePixels global = sprite(3);
        SpritePixels individual = sprite(4);
        TabIconRenderer renderer = new TabIconRenderer(manager, client);
        assertSame(original, renderer.imageFor(widget));
        client.getSpriteOverrides().put(200, global);
        assertEquals(3, renderer.imageFor(widget).getWidth());
        client.getWidgetSpriteOverrides().put(100, individual);
        assertEquals(4, renderer.imageFor(widget).getWidth());
        renderer.imageFor(widget);
        verify(individual, times(1)).toBufferedImage();
        client.getWidgetSpriteOverrides().remove(100);
        assertEquals(3, renderer.imageFor(widget).getWidth());
        client.getSpriteOverrides().clear();
        assertSame(original, renderer.imageFor(widget));
    }

    private static SpritePixels sprite(int size)
    {
        SpritePixels sprite = mock(SpritePixels.class);
        when(sprite.toBufferedImage()).thenReturn(new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB));
        return sprite;
    }
}
