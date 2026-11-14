package com.freddy.customgametabs;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class TabHitRegionsTest
{
    @Test
    public void snapshotsAreDetachedFromMutableOverlayGeometry()
    {
        Rectangle button = new Rectangle(400, 300, 42, 42);
        Map<Integer, Rectangle2D> buttons = new LinkedHashMap<>();
        buttons.put(3, button);
        TabHitRegions snapshot = new TabHitRegions(null, null, buttons);
        button.setLocation(0, 0);
        buttons.clear();
        assertEquals(3, snapshot.tabAt(420, 320));
        assertEquals(-1, snapshot.tabAt(20, 20));
        snapshot.locations().get(3).translate(900, 900);
        assertEquals(new Point(400, 300), snapshot.locations().get(3));
    }

    @Test
    public void transparentGapsPassThroughAndLastPaintedTabWins()
    {
        Map<Integer, Rectangle2D> buttons = new LinkedHashMap<>();
        buttons.put(9, new Rectangle(100, 100, 42, 42));
        buttons.put(2, new Rectangle(120, 100, 42, 42));
        TabHitRegions snapshot = new TabHitRegions(null, new Rectangle(200, 100, 42, 42), buttons);
        assertEquals(2, snapshot.tabAt(130, 110));
        assertFalse(snapshot.contains(180, 110));
        assertTrue(snapshot.handleContains(210, 110));
        assertFalse(TabHitRegions.EMPTY.contains(130, 110));
        assertEquals(-1, snapshot.tabAt(162, 110));
    }
}
