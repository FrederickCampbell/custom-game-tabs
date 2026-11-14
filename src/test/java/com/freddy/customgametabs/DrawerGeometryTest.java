package com.freddy.customgametabs;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class DrawerGeometryTest
{
    @Test
    public void allTabsFitAtMaximumSizeAndSpacingInEveryDirection()
    {
        for (DrawerDirection direction : DrawerDirection.values())
        {
            if (direction == DrawerDirection.AUTO) continue;
            for (int count = 1; count <= 14; count++)
            {
                DrawerGeometry geometry = new DrawerGeometry(direction, count, 84, 20, new Dimension(765, 503));
                Dimension size = geometry.size();
                assertTrue(size.width <= 765);
                assertTrue(size.height <= 503);
                Rectangle frame = new Rectangle(size);
                Set<Point> occupied = new HashSet<>();
                occupied.add(geometry.handleOffset());
                for (int i = 0; i < count; i++)
                {
                    Point p = geometry.tabOffset(i);
                    assertTrue(occupied.add(p));
                    assertTrue(frame.contains(new Rectangle(p.x, p.y, 84, 84)));
                }
            }
        }
    }

    @Test
    public void shortDrawersPreserveExistingGeometry()
    {
        DrawerGeometry geometry = new DrawerGeometry(DrawerDirection.LEFT, 3, 42, 2, new Dimension(800, 600));
        assertEquals(new Point(132, 0), geometry.handleOffset());
        assertEquals(new Point(88, 0), geometry.tabOffset(0));
        assertEquals(new Point(0, 0), geometry.tabOffset(2));
        assertEquals(new Dimension(174, 42), geometry.size());
    }
}
