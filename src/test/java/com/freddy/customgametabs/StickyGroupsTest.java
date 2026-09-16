package com.freddy.customgametabs;

import java.awt.Point;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StickyGroupsTest
{
    @Test
    public void detectsIndependentSnappedClustersAndLeavesLoneTabsAlone()
    {
        final Map<Integer, Point> locations = new LinkedHashMap<>();
        final int cell = 42;
        final int gap = 2;
        final int step = cell + gap;

        locations.put(0, new Point(100, 100));
        locations.put(1, new Point(100 + step, 100));
        locations.put(2, new Point(100 + (2 * step), 100));

        locations.put(6, new Point(400, 300));
        locations.put(7, new Point(400, 300 + step));

        locations.put(13, new Point(700, 500));

        final StickyGroups groups = StickyGroups.detect(locations, cell, gap);

        assertEquals(Arrays.asList(0, 1, 2), groups.membersFor(1));
        assertEquals(Arrays.asList(6, 7), groups.membersFor(6));
        assertEquals(Arrays.asList(13), groups.membersFor(13));
    }

    @Test
    public void groupsRoundTripAndCanBeTrimmedToMainTabs()
    {
        final StickyGroups groups = StickyGroups.parse("0,1,2;6,7");
        assertEquals("0,1,2;6,7", groups.encode());

        final StickyGroups trimmed = groups.retain(Arrays.asList(0, 1, 6, 13));
        assertEquals("0,1", trimmed.encode());
        assertTrue(trimmed.membersFor(6).size() == 1);
    }

    @Test
    public void redetectionMergesNewlySnappedTabIntoExistingCluster()
    {
        final int cell = 42;
        final int gap = 2;
        final int step = cell + gap;
        final Map<Integer, Point> locations = new LinkedHashMap<>();
        locations.put(0, new Point(100, 100));
        locations.put(1, new Point(100 + step, 100));
        locations.put(2, new Point(100 + (2 * step), 100));

        final StickyGroups groups = StickyGroups.detect(locations, cell, gap);
        assertEquals(Arrays.asList(0, 1, 2), groups.membersFor(0));
        assertEquals(Arrays.asList(0, 1, 2), groups.membersFor(2));
    }
}
