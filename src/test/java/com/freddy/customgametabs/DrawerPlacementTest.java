package com.freddy.customgametabs;

import java.awt.Dimension;
import java.awt.Point;
import org.junit.Test;
import static org.junit.Assert.*;

public class DrawerPlacementTest
{
    @Test
    public void wrappedDirectionUsesFullTwoDimensionalFootprint()
    {
        Dimension canvas = new Dimension(765, 503);
        // Four runs fit horizontally; a 14-tab straight line does not.
        assertEquals(DrawerDirection.RIGHT, DrawerPlacement.choose(DrawerDirection.RIGHT,
            new Point(0, 0), null, 14, 84, 20, canvas));
        // Cross-axis footprint matters as well as the requested expansion axis.
        DrawerDirection result = DrawerPlacement.choose(DrawerDirection.RIGHT,
            new Point(0, 419), null, 14, 84, 20, canvas);
        assertEquals(DrawerDirection.UP, result);
        DrawerGeometry geometry = new DrawerGeometry(result, 14, 84, 20, canvas);
        Point offset = geometry.handleOffset();
        Dimension size = geometry.size();
        assertTrue(419 - offset.y + size.height <= canvas.height);
        assertTrue(size.width <= canvas.width);
    }
}
