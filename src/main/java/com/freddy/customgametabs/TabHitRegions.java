package com.freddy.customgametabs;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable, absolute logical-canvas geometry published once per render. */
final class TabHitRegions
{
    static final TabHitRegions EMPTY = new TabHitRegions(null, null, new LinkedHashMap<>());

    private final Rectangle2D surface;
    private final Rectangle2D handle;
    private final Map<Integer, Rectangle2D> tabs = new LinkedHashMap<>();

    TabHitRegions(Rectangle2D surface, Rectangle2D handle, Map<Integer, ? extends Rectangle2D> tabs)
    {
        this.surface = copy(surface);
        this.handle = copy(handle);
        tabs.forEach((tab, bounds) -> this.tabs.put(tab, copy(bounds)));
    }

    int tabAt(int x, int y)
    {
        // Later painted buttons win during overlapping drawer animation.
        int hit = -1;
        for (Map.Entry<Integer, Rectangle2D> entry : tabs.entrySet())
        {
            if (entry.getValue().contains(x, y))
            {
                hit = entry.getKey();
            }
        }
        return hit;
    }

    boolean handleContains(int x, int y)
    {
        return handle != null && handle.contains(x, y);
    }

    boolean contains(int x, int y)
    {
        return (surface != null && surface.contains(x, y))
            || handleContains(x, y) || tabAt(x, y) >= 0;
    }

    Point handleLocation()
    {
        return handle == null ? null : location(handle);
    }

    Map<Integer, Point> locations()
    {
        final Map<Integer, Point> result = new LinkedHashMap<>();
        tabs.forEach((tab, bounds) -> result.put(tab, location(bounds)));
        return result;
    }

    private static Point location(Rectangle2D bounds)
    {
        return new Point((int) Math.round(bounds.getX()), (int) Math.round(bounds.getY()));
    }

    private static Rectangle2D copy(Rectangle2D bounds)
    {
        return bounds == null ? null : new Rectangle2D.Double(
            bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }
}
