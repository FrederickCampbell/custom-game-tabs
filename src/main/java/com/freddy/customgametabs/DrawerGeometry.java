package com.freddy.customgametabs;

import java.awt.Dimension;
import java.awt.Point;

/** Wrap drawer tabs along the expansion axis so a long drawer fits the canvas. */
final class DrawerGeometry
{
    private final DrawerDirection direction;
    private final int count;
    private final int cell;
    private final int gap;
    private final int run;
    private final int cross;

    DrawerGeometry(DrawerDirection direction, int count, int cell, int gap, Dimension canvas)
    {
        this.direction = direction;
        this.count = Math.max(0, count);
        this.cell = Math.max(1, cell);
        this.gap = Math.max(0, gap);
        final int available = horizontal() ? canvas.width : canvas.height;
        final int capacity = Math.max(1, (available - this.cell) / step());
        run = Math.min(this.count, capacity);
        cross = run == 0 ? 0 : (this.count + run - 1) / run;
    }

    Point handleOffset()
    {
        return new Point(direction == DrawerDirection.LEFT ? run * step() : 0,
            direction == DrawerDirection.UP ? run * step() : 0);
    }

    Point tabOffset(int index)
    {
        if (index < 0 || index >= count)
        {
            throw new IllegalArgumentException("Drawer tab index out of range");
        }
        final int along = index % run;
        final int across = index / run;
        final int offset = (direction == DrawerDirection.LEFT || direction == DrawerDirection.UP)
            ? (run - along - 1) * step() : (along + 1) * step();
        return horizontal() ? new Point(offset, across * step()) : new Point(across * step(), offset);
    }

    Dimension size()
    {
        final int along = cell + run * step();
        final int across = Math.max(cell, cross * step() - gap);
        return horizontal() ? new Dimension(along, across) : new Dimension(across, along);
    }

    private int step()
    {
        return cell + gap;
    }

    private boolean horizontal()
    {
        return direction == DrawerDirection.LEFT || direction == DrawerDirection.RIGHT;
    }
}
