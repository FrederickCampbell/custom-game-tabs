package com.freddy.verticaltabs;

import java.util.LinkedHashSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LayoutTest
{
    @Test
    public void modernTabDefinitionsStayAligned()
    {
        assertEquals(14, LayoutSpec.TABS.length);
        assertEquals("Inventory", LayoutSpec.TABS[LayoutSpec.INVENTORY_TAB].getName());
        assertEquals("Logout", LayoutSpec.TABS[LayoutSpec.LOGOUT_TAB].getName());
        assertEquals("Friends Chat", LayoutSpec.TABS[7].getName());
        assertEquals("Ignore List", LayoutSpec.TABS[8].getName());
        assertEquals("Friends List", LayoutSpec.TABS[9].getName());

        assertTrue(LayoutSpec.isValidTab(0));
        assertTrue(LayoutSpec.isValidTab(13));
        assertFalse(LayoutSpec.isValidTab(-1));
        assertFalse(LayoutSpec.isValidTab(14));
    }

    @Test
    public void scalingPreservesSubpixelSizes()
    {
        assertEquals(21.84, DockMetrics.scaleExact(42, 52), 0.0001);
        assertEquals(22.26, DockMetrics.scaleExact(42, 53), 0.0001);
        assertEquals(22, (int) Math.ceil(DockMetrics.scaleExact(42, 52)));
        assertEquals(23, (int) Math.ceil(DockMetrics.scaleExact(42, 53)));

        assertEquals(3, DockMetrics.gapPixels(3));
        assertEquals(0, DockMetrics.gapPixels(-10));
    }

    @Test
    public void customOrderParserDeduplicatesAndKeepsOrder()
    {
        final LinkedHashSet<Integer> parsed = TabLayout.parse(
            "inventory, prayer, friends, ignore, inventory"
        );

        assertEquals(4, parsed.size());
        assertEquals(
            Integer.valueOf(LayoutSpec.INVENTORY_TAB),
            parsed.iterator().next()
        );
        assertTrue(parsed.contains(5));
        assertTrue(parsed.contains(9));
        assertTrue(parsed.contains(8));
    }

    @Test
    public void customOrderParserAcceptsFriendlyAliases()
    {
        final LinkedHashSet<Integer> parsed = TabLayout.parse(
            "bag; spellbook\nsettings, exit"
        );

        assertTrue(parsed.contains(LayoutSpec.INVENTORY_TAB));
        assertTrue(parsed.contains(6));
        assertTrue(parsed.contains(11));
        assertTrue(parsed.contains(LayoutSpec.LOGOUT_TAB));
    }
}
