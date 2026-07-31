package com.freddy.verticaltabs;

import java.util.LinkedHashSet;

public final class LayoutSelfTest
{
    private LayoutSelfTest()
    {
    }

    public static void main(String[] args)
    {
        assert LayoutSpec.TABS.length == 14;
        assert LayoutSpec.TABS[7].getName().equals("Friends Chat");
        assert LayoutSpec.TABS[8].getName().equals("Ignore List");
        assert LayoutSpec.TABS[9].getName().equals("Friends List");

        assert Math.abs(DockMetrics.scaleExact(42, 52) - 21.84) < 0.0001;
        assert Math.abs(DockMetrics.scaleExact(42, 53) - 22.26) < 0.0001;
        assert (int) Math.ceil(DockMetrics.scaleExact(42, 52)) == 22;
        assert (int) Math.ceil(DockMetrics.scaleExact(42, 53)) == 23;
        assert DockMetrics.gapPixels(3) == 3;
        assert DockMetrics.gapPixels(4) == 4;
        assert DockMetrics.gapPixels(5) == 5;
        assert DockMetrics.gapPixels(6) == 6;

        final LinkedHashSet<Integer> parsed =
            TabLayout.parse(
                "inventory, prayer, friends, ignore, inventory"
            );
        assert parsed.size() == 4;
        assert parsed.iterator().next() == 3;
        assert parsed.contains(5);
        assert parsed.contains(9);
        assert parsed.contains(8);
    }
}
