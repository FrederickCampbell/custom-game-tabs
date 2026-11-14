package com.freddy.customgametabs;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

/** Direction selection uses exactly the same wrapped extent as the renderer. */
final class DrawerPlacement
{
    static Point handle(DrawerDirection direction, Rectangle dock, int cell, int gap)
    {
        switch (direction)
        {
            case RIGHT: return new Point(dock.x + dock.width + gap, dock.y);
            case UP: return new Point(dock.x, dock.y - cell - gap);
            case DOWN: return new Point(dock.x, dock.y + dock.height + gap);
            default: return new Point(dock.x - cell - gap, dock.y);
        }
    }

    static DrawerDirection choose(DrawerDirection requested, Point handle, Rectangle dock,
        int count, int cell, int gap, Dimension canvas)
    {
        DrawerDirection[] choices = dock != null && dock.width > dock.height
            ? new DrawerDirection[] {DrawerDirection.UP, DrawerDirection.DOWN, DrawerDirection.LEFT, DrawerDirection.RIGHT}
            : new DrawerDirection[] {DrawerDirection.LEFT, DrawerDirection.RIGHT, DrawerDirection.UP, DrawerDirection.DOWN};
        if (requested != DrawerDirection.AUTO)
        {
            DrawerDirection opposite = requested == DrawerDirection.LEFT ? DrawerDirection.RIGHT
                : requested == DrawerDirection.RIGHT ? DrawerDirection.LEFT
                : requested == DrawerDirection.UP ? DrawerDirection.DOWN : DrawerDirection.UP;
            choices = new DrawerDirection[] {requested, opposite, choices[0], choices[1], choices[2], choices[3]};
        }
        DrawerDirection best = choices[0];
        long leastMovement = Long.MAX_VALUE;
        for (DrawerDirection direction : choices)
        {
            DrawerGeometry geometry = new DrawerGeometry(direction, count, cell, gap, canvas);
            Point anchor = dock == null ? handle : handle(direction, dock, cell, gap);
            Point offset = geometry.handleOffset();
            Dimension size = geometry.size();
            int x = anchor.x - offset.x;
            int y = anchor.y - offset.y;
            int clampedX = Math.max(0, Math.min(x, Math.max(0, canvas.width - size.width)));
            int clampedY = Math.max(0, Math.min(y, Math.max(0, canvas.height - size.height)));
            long movement = Math.abs((long) x - clampedX) + Math.abs((long) y - clampedY)
                + Math.max(0, size.width - canvas.width) + Math.max(0, size.height - canvas.height);
            if (movement < leastMovement)
            {
                leastMovement = movement;
                best = direction;
            }
        }
        return best;
    }
}
