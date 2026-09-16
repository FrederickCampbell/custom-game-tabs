package com.freddy.customgametabs;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ActivityLayoutSnapshotTest
{
    @Test
    public void completeActivityLayoutRoundTrips()
    {
        final List<Integer> order = new ArrayList<>();
        for (int i = 13; i >= 0; i--)
        {
            order.add(i);
        }

        final TabState[] states = new TabState[14];
        for (int i = 0; i < states.length; i++)
        {
            states[i] = i % 3 == 0
                ? TabState.DRAWER
                : i % 3 == 1 ? TabState.HIDDEN : TabState.MAIN;
        }

        final Map<Integer, Point> locations = new LinkedHashMap<>();
        locations.put(0, new Point(100, 200));
        locations.put(6, new Point(300, 400));

        final ActivityLayoutSnapshot original = new ActivityLayoutSnapshot(
            1000,
            800,
            TabLayoutMode.FREEFORM,
            4,
            order,
            states,
            true,
            DrawerDirection.UP,
            false,
            true,
            new Point(777, 222),
            true,
            StickyGroups.parse("0,6;2,3"),
            locations,
            new ActivityLayoutSnapshot.Appearance(
                137,
                7,
                33,
                88,
                97,
                61,
                true,
                new Color(10, 20, 30, 40),
                new Color(50, 60, 70, 80),
                new Color(90, 100, 110, 120),
                new Color(130, 140, 150, 160)
            )
        );

        final ActivityLayoutSnapshot parsed = ActivityLayoutSnapshot.parse(original.encode());
        assertNotNull(parsed);
        assertFalse(parsed.isLegacyPlacementOnly());
        assertEquals(TabLayoutMode.FREEFORM, parsed.getLayoutMode());
        assertEquals(4, parsed.getButtonsPerRow());
        assertEquals(order, parsed.getOrder());
        assertEquals(TabState.DRAWER, parsed.getState(0));
        assertEquals(TabState.HIDDEN, parsed.getState(1));
        assertEquals(TabState.MAIN, parsed.getState(2));
        assertTrue(parsed.isDrawerEnabled());
        assertEquals(DrawerDirection.UP, parsed.getDrawerDirection());
        assertFalse(parsed.isDrawerCloseAfterSelection());
        assertTrue(parsed.isDrawerExpanded());
        assertTrue(parsed.isStickTogether());
        assertEquals("0,6;2,3", parsed.getStickyGroups().encode());
        assertTrue(parsed.hasAppearance());
        final ActivityLayoutSnapshot.Appearance appearance = parsed.getAppearance();
        assertEquals(137, appearance.getButtonScale());
        assertEquals(7, appearance.getGap());
        assertEquals(33, appearance.getIdleOpacity());
        assertEquals(88, appearance.getHoverOpacity());
        assertEquals(97, appearance.getSelectedOpacity());
        assertEquals(61, appearance.getFrameOpacity());
        assertTrue(appearance.isShowTooltip());
        assertEquals(new Color(10, 20, 30, 40), appearance.getButtonColor());
        assertEquals(new Color(50, 60, 70, 80), appearance.getHoverColor());
        assertEquals(new Color(90, 100, 110, 120), appearance.getSelectedColor());
        assertEquals(new Color(130, 140, 150, 160), appearance.getBorderColor());
        assertTrue(original.encode().startsWith("3|"));
        assertEquals(locations, parsed.resolveLocations(1000, 800, 42));
        assertEquals(new Point(777, 222), parsed.resolveDrawerHandle(1000, 800, 42));
    }

    @Test
    public void legacyPositionOnlyPresetStillLoadsWithoutActivityState()
    {
        final ActivityLayoutSnapshot legacy = ActivityLayoutSnapshot.parse(
            "1|1000|800|0,100,200;6,300,400"
        );

        assertNotNull(legacy);
        assertTrue(legacy.isLegacyPlacementOnly());
        assertFalse(legacy.hasAppearance());
        assertEquals(TabLayoutMode.FREEFORM, legacy.getLayoutMode());
        assertEquals(new Point(100, 200), legacy.resolveLocations(1000, 800, 42).get(0));
        assertEquals(new Point(300, 400), legacy.resolveLocations(1000, 800, 42).get(6));
    }

    @Test
    public void version2PresetStillLoadsWithoutInventingAppearance()
    {
        final String v2 = "2|1000|800|FREEFORM|4|"
            + "0,1,2,3,4,5,6,7,8,9,10,11,12,13|"
            + "MMMMMMMMMMMMMM|true|UP|false|true|777,222|true|0,1|"
            + "0,100,200;1,144,200";

        final ActivityLayoutSnapshot parsed = ActivityLayoutSnapshot.parse(v2);
        assertNotNull(parsed);
        assertFalse(parsed.isLegacyPlacementOnly());
        assertFalse(parsed.hasAppearance());
        assertEquals(v2, parsed.encode());
        assertEquals(new Point(100, 200), parsed.resolveLocations(1000, 800, 42).get(0));
        assertEquals(new Point(144, 200), parsed.resolveLocations(1000, 800, 42).get(1));
    }

    @Test
    public void locationsAndDrawerHandleScaleAndClampToNewCanvas()
    {
        final TabState[] states = new TabState[14];
        for (int i = 0; i < states.length; i++)
        {
            states[i] = TabState.MAIN;
        }

        final Map<Integer, Point> locations = new LinkedHashMap<>();
        locations.put(0, new Point(990, 790));

        final ActivityLayoutSnapshot snapshot = new ActivityLayoutSnapshot(
            1000,
            800,
            TabLayoutMode.FREEFORM,
            1,
            null,
            states,
            true,
            DrawerDirection.AUTO,
            true,
            false,
            new Point(999, 799),
            false,
            new StickyGroups(),
            locations
        );

        assertEquals(new Point(1958, 1558), snapshot.resolveLocations(2000, 1600, 42).get(0));
        assertEquals(new Point(1958, 1558), snapshot.resolveDrawerHandle(2000, 1600, 42));
    }

    @Test
    public void resizeReanchorsFreeformAsRigidShapeWithoutStretchingGaps()
    {
        final TabState[] states = new TabState[14];
        for (int i = 0; i < states.length; i++)
        {
            states[i] = TabState.MAIN;
        }

        final Map<Integer, Point> locations = new LinkedHashMap<>();
        locations.put(0, new Point(100, 120));
        locations.put(1, new Point(144, 120));
        locations.put(2, new Point(144, 164));

        final ActivityLayoutSnapshot snapshot = new ActivityLayoutSnapshot(
            1000,
            800,
            TabLayoutMode.FREEFORM,
            1,
            null,
            states,
            true,
            DrawerDirection.AUTO,
            true,
            false,
            new Point(190, 120),
            false,
            new StickyGroups(),
            locations
        );

        final Map<Integer, Point> resized = snapshot.resolveLocations(1800, 1200, 42);
        assertEquals(44, resized.get(1).x - resized.get(0).x);
        assertEquals(0, resized.get(1).y - resized.get(0).y);
        assertEquals(0, resized.get(2).x - resized.get(1).x);
        assertEquals(44, resized.get(2).y - resized.get(1).y);

        final Point drawer = snapshot.resolveDrawerHandle(1800, 1200, 42);
        assertEquals(90, drawer.x - resized.get(0).x);
        assertEquals(0, drawer.y - resized.get(0).y);
    }

    @Test
    public void rightAnchoredStickyGroupPreservesEdgeGapAndInternalSpacing()
    {
        final TabState[] states = allMainStates();
        final Map<Integer, Point> locations = new LinkedHashMap<>();
        locations.put(0, new Point(872, 100));
        locations.put(1, new Point(916, 100));

        final ActivityLayoutSnapshot snapshot = new ActivityLayoutSnapshot(
            1000,
            800,
            TabLayoutMode.FREEFORM,
            1,
            null,
            states,
            false,
            DrawerDirection.AUTO,
            true,
            false,
            null,
            true,
            StickyGroups.parse("0,1"),
            locations
        );

        final Map<Integer, Point> wider = snapshot.resolveLocations(1600, 800, 42);
        assertEquals(new Point(1472, 100), wider.get(0));
        assertEquals(new Point(1516, 100), wider.get(1));
        assertEquals(42, 1600 - (wider.get(1).x + 42));
        assertEquals(44, wider.get(1).x - wider.get(0).x);

        final Map<Integer, Point> narrower = snapshot.resolveLocations(700, 800, 42);
        assertEquals(new Point(572, 100), narrower.get(0));
        assertEquals(new Point(616, 100), narrower.get(1));
        assertEquals(42, 700 - (narrower.get(1).x + 42));
        assertEquals(44, narrower.get(1).x - narrower.get(0).x);
    }

    @Test
    public void separateLeftAndRightComponentsAnchorIndependently()
    {
        final TabState[] states = allMainStates();
        final Map<Integer, Point> locations = new LinkedHashMap<>();
        locations.put(0, new Point(20, 100));
        locations.put(1, new Point(64, 100));
        locations.put(2, new Point(872, 100));
        locations.put(3, new Point(916, 100));

        final ActivityLayoutSnapshot snapshot = new ActivityLayoutSnapshot(
            1000,
            800,
            TabLayoutMode.FREEFORM,
            1,
            null,
            states,
            false,
            DrawerDirection.AUTO,
            true,
            false,
            null,
            true,
            StickyGroups.parse("0,1;2,3"),
            locations
        );

        final Map<Integer, Point> resized = snapshot.resolveLocations(1400, 800, 42);
        assertEquals(new Point(20, 100), resized.get(0));
        assertEquals(new Point(64, 100), resized.get(1));
        assertEquals(new Point(1272, 100), resized.get(2));
        assertEquals(new Point(1316, 100), resized.get(3));
    }

    @Test
    public void tooNarrowCanvasDoesNotCollapseStickyMembersOntoEachOther()
    {
        final TabState[] states = allMainStates();
        final Map<Integer, Point> locations = new LinkedHashMap<>();
        locations.put(0, new Point(700, 100));
        locations.put(1, new Point(744, 100));
        locations.put(2, new Point(788, 100));

        final ActivityLayoutSnapshot snapshot = new ActivityLayoutSnapshot(
            1000,
            800,
            TabLayoutMode.FREEFORM,
            1,
            null,
            states,
            false,
            DrawerDirection.AUTO,
            true,
            false,
            null,
            true,
            StickyGroups.parse("0,1,2"),
            locations
        );

        final Map<Integer, Point> resized = snapshot.resolveLocations(80, 800, 42);
        assertEquals(44, resized.get(1).x - resized.get(0).x);
        assertEquals(44, resized.get(2).x - resized.get(1).x);
    }

    private static TabState[] allMainStates()
    {
        final TabState[] states = new TabState[14];
        for (int i = 0; i < states.length; i++)
        {
            states[i] = TabState.MAIN;
        }
        return states;
    }

    @Test
    public void malformedSnapshotsAreRejected()
    {
        assertNull(ActivityLayoutSnapshot.parse(null));
        assertNull(ActivityLayoutSnapshot.parse(""));
        assertNull(ActivityLayoutSnapshot.parse("3|100|100"));
        assertNull(ActivityLayoutSnapshot.parse("2|bad|100|FREEFORM|1||||||||||"));
        assertNull(ActivityLayoutSnapshot.parse(
            "3|100|100|FREEFORM|1|0,1,2,3,4,5,6,7,8,9,10,11,12,13|"
                + "MMMMMMMMMMMMMM|true|AUTO|true|false||false|||bad-appearance"
        ));
    }
}
