package com.freddy.customgametabs;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Lightweight snapshot of freeform clusters that should move as units. */
final class StickyGroups
{
    private static final int TOLERANCE = 3;

    private final List<LinkedHashSet<Integer>> groups;

    StickyGroups()
    {
        this.groups = new ArrayList<>();
    }

    private StickyGroups(List<LinkedHashSet<Integer>> groups)
    {
        this.groups = groups;
    }

    static StickyGroups detect(
        Map<Integer, Point> locations,
        int cellSize,
        int gap
    )
    {
        final List<Integer> tabs = new ArrayList<>(locations.keySet());
        final Map<Integer, Set<Integer>> adjacency = new LinkedHashMap<>();

        for (int tab : tabs)
        {
            adjacency.put(tab, new LinkedHashSet<>());
        }

        for (int i = 0; i < tabs.size(); i++)
        {
            final int a = tabs.get(i);
            final Point pa = locations.get(a);

            for (int j = i + 1; j < tabs.size(); j++)
            {
                final int b = tabs.get(j);
                final Point pb = locations.get(b);

                if (adjacent(pa, pb, cellSize, gap))
                {
                    adjacency.get(a).add(b);
                    adjacency.get(b).add(a);
                }
            }
        }

        final Set<Integer> visited = new LinkedHashSet<>();
        final List<LinkedHashSet<Integer>> detected = new ArrayList<>();

        for (int start : tabs)
        {
            if (!visited.add(start))
            {
                continue;
            }

            final LinkedHashSet<Integer> component = new LinkedHashSet<>();
            final ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(start);

            while (!queue.isEmpty())
            {
                final int current = queue.removeFirst();
                component.add(current);

                for (int next : adjacency.get(current))
                {
                    if (visited.add(next))
                    {
                        queue.addLast(next);
                    }
                }
            }

            if (component.size() > 1)
            {
                detected.add(component);
            }
        }

        return new StickyGroups(detected);
    }

    static StickyGroups parse(String encoded)
    {
        final StickyGroups result = new StickyGroups();
        if (encoded == null || encoded.isBlank())
        {
            return result;
        }

        final List<LinkedHashSet<Integer>> parsed = new ArrayList<>();
        for (String rawGroup : encoded.split(";"))
        {
            final LinkedHashSet<Integer> group = new LinkedHashSet<>();
            for (String rawTab : rawGroup.split(","))
            {
                try
                {
                    final int tab = Integer.parseInt(rawTab.trim());
                    if (LayoutSpec.isValidTab(tab))
                    {
                        group.add(tab);
                    }
                }
                catch (NumberFormatException ignored)
                {
                    // Ignore malformed historical/custom data.
                }
            }

            if (group.size() > 1)
            {
                parsed.add(group);
            }
        }

        return new StickyGroups(parsed);
    }

    String encode()
    {
        final StringBuilder out = new StringBuilder();
        boolean firstGroup = true;

        for (Set<Integer> group : groups)
        {
            if (!firstGroup)
            {
                out.append(';');
            }

            boolean firstTab = true;
            for (int tab : group)
            {
                if (!firstTab)
                {
                    out.append(',');
                }
                out.append(tab);
                firstTab = false;
            }

            firstGroup = false;
        }

        return out.toString();
    }

    boolean isEmpty()
    {
        return groups.isEmpty();
    }

    List<Integer> membersFor(int tabIndex)
    {
        for (Set<Integer> group : groups)
        {
            if (group.contains(tabIndex))
            {
                return new ArrayList<>(group);
            }
        }

        return Collections.singletonList(tabIndex);
    }

    StickyGroups retain(Collection<Integer> allowed)
    {
        final Set<Integer> permitted = new LinkedHashSet<>(allowed);
        final List<LinkedHashSet<Integer>> filtered = new ArrayList<>();

        for (Set<Integer> group : groups)
        {
            final LinkedHashSet<Integer> kept = new LinkedHashSet<>();
            for (int tab : group)
            {
                if (permitted.contains(tab))
                {
                    kept.add(tab);
                }
            }

            if (kept.size() > 1)
            {
                filtered.add(kept);
            }
        }

        return new StickyGroups(filtered);
    }

    List<List<Integer>> asLists()
    {
        final List<List<Integer>> out = new ArrayList<>();
        for (Set<Integer> group : groups)
        {
            out.add(new ArrayList<>(group));
        }
        return out;
    }

    private static boolean adjacent(
        Point a,
        Point b,
        int cellSize,
        int gap
    )
    {
        final int step = Math.max(1, cellSize) + Math.max(0, gap);
        final boolean horizontal =
            Math.abs(Math.abs(a.x - b.x) - step) <= TOLERANCE
                && Math.abs(a.y - b.y) <= TOLERANCE;
        final boolean vertical =
            Math.abs(Math.abs(a.y - b.y) - step) <= TOLERANCE
                && Math.abs(a.x - b.x) <= TOLERANCE;
        return horizontal || vertical;
    }
}
