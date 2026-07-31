package com.freddy.verticaltabs;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;

@Singleton
final class LooseSnapManager
{
    private static final String LINKS_KEY = "snapLinks";
    private static final int RESTORE_STABLE_TICKS = 2;

    private final VerticalTabsConfig config;
    private final ConfigManager configManager;
    private final OverlayManager overlayManager;

    private final Map<Integer, IndividualTabOverlay> overlays =
        new LinkedHashMap<>();
    private final Map<Integer, SnapLink> links =
        new LinkedHashMap<>();
    private final Map<Integer, Rectangle> restoreBounds =
        new HashMap<>();

    private boolean restorePending;
    private int restoreStableTicks;

    @Inject
    LooseSnapManager(
        VerticalTabsConfig config,
        ConfigManager configManager,
        OverlayManager overlayManager
    )
    {
        this.config = config;
        this.configManager = configManager;
        this.overlayManager = overlayManager;
    }

    void setOverlays(List<IndividualTabOverlay> newOverlays)
    {
        overlays.clear();

        for (IndividualTabOverlay overlay : newOverlays)
        {
            overlays.put(overlay.getTabIndex(), overlay);
        }

        loadLinks();
        pruneLinks();
        scheduleRestoredLayout();
    }

    void clearOverlays()
    {
        overlays.clear();
        restoreBounds.clear();
        restorePending = false;
        restoreStableTicks = 0;
    }

    void onPostClientTick()
    {
        if (
            !restorePending
                || !config.moveSeparately()
                || overlays.isEmpty()
        )
        {
            return;
        }

        if (!captureStableRenderedBounds())
        {
            restoreStableTicks = 0;
            return;
        }

        restoreStableTicks++;

        if (restoreStableTicks < RESTORE_STABLE_TICKS)
        {
            return;
        }

        restorePending = false;
        restoreStableTicks = 0;
        restoreBounds.clear();
        reflowAll();
    }

    private void scheduleRestoredLayout()
    {
        restorePending =
            config.moveSeparately() && !overlays.isEmpty();
        restoreStableTicks = 0;
        restoreBounds.clear();

        for (IndividualTabOverlay overlay : overlays.values())
        {
            overlay.revalidate();
        }
    }

    /*
     * OverlayManager loads each saved preferred location synchronously when an
     * overlay is added, but OverlayRenderer does not apply that location to
     * getBounds() until the overlay has actually rendered. Waiting for two
     * identical, non-empty bound snapshots prevents startup/profile reflow
     * from anchoring a snapped group to temporary TOP_RIGHT/default bounds.
     */
    private boolean captureStableRenderedBounds()
    {
        boolean stable = !restoreBounds.isEmpty();

        for (Map.Entry<Integer, IndividualTabOverlay> entry :
            overlays.entrySet())
        {
            final Rectangle current = entry.getValue().getBounds();

            if (current.width <= 0 || current.height <= 0)
            {
                restoreBounds.clear();
                return false;
            }

            final Rectangle previous = restoreBounds.put(
                entry.getKey(),
                new Rectangle(current)
            );

            if (previous == null || !previous.equals(current))
            {
                stable = false;
            }
        }

        return stable && restoreBounds.size() == overlays.size();
    }

    void isolate(int tabIndex)
    {
        if (isolateLinks(tabIndex))
        {
            saveLinks();
        }
    }

    /*
     * Make one grabbed button independent without destroying the rest of a
     * snapped layout. Remove both its parent link and every direct child link.
     * The detached child branches keep their own descendants, so the remaining
     * buttons stay exactly where they were instead of collapsing or mixing.
     */
    private boolean isolateLinks(int tabIndex)
    {
        boolean changed = links.remove(tabIndex) != null;

        for (Integer child : new ArrayList<>(links.keySet()))
        {
            final SnapLink link = links.get(child);

            if (link != null && link.parent == tabIndex)
            {
                links.remove(child);
                changed = true;
            }
        }

        return changed;
    }

    void clearLinks()
    {
        if (links.isEmpty())
        {
            return;
        }

        links.clear();
        saveLinks();
    }

    void configChanged(String key)
    {
        if ("snapLooseButtons".equals(key))
        {
            if (!config.snapLooseButtons())
            {
                clearLinks();
            }

            return;
        }

        if (
            "gap".equals(key)
                || "buttonScale".equals(key)
        )
        {
            reflowSoon();
        }
    }

    boolean snap(
        IndividualTabOverlay target,
        IndividualTabOverlay moving
    )
    {
        if (
            !config.snapLooseButtons()
                || target == moving
        )
        {
            return false;
        }

        final int child = moving.getTabIndex();

        /*
         * A drop always moves one button, never an old attached subtree. This
         * also repairs a missed Alt-press when overlapping overlays made the
         * mouse hit test ambiguous.
         */
        final boolean isolated = isolateLinks(child);
        final SnapDirection direction =
            directionFor(target.getBounds(), moving.getBounds());

        int parent = target.getTabIndex();

        /*
         * Repeatedly dropping onto an occupied side extends the row/column
         * instead of placing two buttons on top of each other.
         */
        for (int guard = 0; guard < LayoutSpec.TABS.length; guard++)
        {
            final Integer occupant =
                childOnSide(parent, direction);

            if (occupant == null || occupant == child)
            {
                break;
            }

            parent = occupant;
        }

        if (wouldCreateCycle(child, parent))
        {
            if (isolated)
            {
                saveLinks();
            }

            return false;
        }

        links.put(child, new SnapLink(parent, direction));
        saveLinks();
        reflowSoon();
        return true;
    }

    private void reflowSoon()
    {
        restorePending = false;
        restoreStableTicks = 0;
        restoreBounds.clear();

        if (!config.moveSeparately() || overlays.isEmpty())
        {
            return;
        }

        SwingUtilities.invokeLater(() ->
        {
            for (IndividualTabOverlay overlay : overlays.values())
            {
                overlay.revalidate();
            }

            SwingUtilities.invokeLater(this::reflowAll);
        });
    }

    private void reflowAll()
    {
        if (!config.moveSeparately() || overlays.isEmpty())
        {
            return;
        }

        pruneLinks();

        final int size = DockMetrics.buttonCellSize(config);
        final int gap = DockMetrics.gap(config);
        final Map<Integer, Point> planned = new HashMap<>();

        for (int tabIndex : overlays.keySet())
        {
            resolve(
                tabIndex,
                planned,
                new HashSet<>(),
                size,
                gap
            );
        }

        for (Map.Entry<Integer, SnapLink> entry : links.entrySet())
        {
            final IndividualTabOverlay overlay =
                overlays.get(entry.getKey());
            final Point desired = planned.get(entry.getKey());

            if (overlay != null && desired != null)
            {
                moveToCanvas(overlay, desired);
            }
        }
    }

    private Point resolve(
        int tabIndex,
        Map<Integer, Point> planned,
        Set<Integer> visiting,
        int size,
        int gap
    )
    {
        final Point existing = planned.get(tabIndex);
        if (existing != null)
        {
            return existing;
        }

        final IndividualTabOverlay overlay = overlays.get(tabIndex);
        if (overlay == null)
        {
            return null;
        }

        if (!visiting.add(tabIndex))
        {
            final Rectangle bounds = overlay.getBounds();
            final Point fallback = new Point(bounds.x, bounds.y);
            planned.put(tabIndex, fallback);
            return fallback;
        }

        final SnapLink link = links.get(tabIndex);
        final Point result;

        if (link == null || !overlays.containsKey(link.parent))
        {
            final Rectangle bounds = overlay.getBounds();
            result = new Point(bounds.x, bounds.y);
        }
        else
        {
            final Point parent = resolve(
                link.parent,
                planned,
                visiting,
                size,
                gap
            );

            result = parent == null
                ? new Point(
                    overlay.getBounds().x,
                    overlay.getBounds().y
                )
                : offset(parent, link.direction, size, gap);
        }

        visiting.remove(tabIndex);
        planned.put(tabIndex, result);
        return result;
    }

    private void moveToCanvas(
        IndividualTabOverlay overlay,
        Point desired
    )
    {
        final Point preferred = overlay.getPreferredLocation();

        /*
         * Snapped buttons have been Alt-dragged at least once, so RuneLite
         * normally gives them a saved preferred location. Wait for the next
         * reflow rather than guessing an anchor when it is not ready yet.
         */
        if (preferred == null)
        {
            return;
        }

        final Rectangle current = overlay.getBounds();

        if (
            current.x == desired.x
                && current.y == desired.y
        )
        {
            return;
        }

        final Point adjusted = new Point(preferred);
        adjusted.translate(
            desired.x - current.x,
            desired.y - current.y
        );

        overlay.setPreferredPosition(null);
        overlay.setPreferredLocation(adjusted);
        overlay.revalidate();
        overlayManager.saveOverlay(overlay);
    }

    private void loadLinks()
    {
        links.clear();

        final String stored =
            configManager.getConfiguration(
                VerticalTabsConfig.GROUP,
                LINKS_KEY
            );

        if (stored == null || stored.isBlank())
        {
            return;
        }

        for (String rawLink : stored.split(";"))
        {
            final String[] parts = rawLink.split(":");

            if (parts.length != 3)
            {
                continue;
            }

            try
            {
                final int child = Integer.parseInt(parts[0]);
                final int parent = Integer.parseInt(parts[1]);
                final SnapDirection direction =
                    SnapDirection.valueOf(parts[2]);

                if (
                    child >= 0
                        && child < LayoutSpec.TABS.length
                        && parent >= 0
                        && parent < LayoutSpec.TABS.length
                        && child != parent
                )
                {
                    links.put(
                        child,
                        new SnapLink(parent, direction)
                    );
                }
            }
            catch (
                IllegalArgumentException ignored
            )
            {
                // Ignore malformed or obsolete saved entries.
            }
        }
    }

    private void saveLinks()
    {
        final List<String> serialized = new ArrayList<>();

        for (Map.Entry<Integer, SnapLink> entry : links.entrySet())
        {
            final SnapLink link = entry.getValue();

            serialized.add(
                entry.getKey()
                    + ":"
                    + link.parent
                    + ":"
                    + link.direction.name()
            );
        }

        configManager.setConfiguration(
            VerticalTabsConfig.GROUP,
            LINKS_KEY,
            String.join(";", serialized)
        );
    }

    private void pruneLinks()
    {
        boolean changed = false;

        for (Integer child : new ArrayList<>(links.keySet()))
        {
            final SnapLink link = links.get(child);

            if (
                !overlays.containsKey(child)
                    || link == null
                    || !overlays.containsKey(link.parent)
                    || child == link.parent
                    || wouldCreateCycle(child, link.parent)
            )
            {
                links.remove(child);
                changed = true;
            }
        }

        if (changed)
        {
            saveLinks();
        }
    }

    private Integer childOnSide(
        int parent,
        SnapDirection direction
    )
    {
        for (Map.Entry<Integer, SnapLink> entry : links.entrySet())
        {
            final SnapLink link = entry.getValue();

            if (
                link.parent == parent
                    && link.direction == direction
            )
            {
                return entry.getKey();
            }
        }

        return null;
    }

    private boolean wouldCreateCycle(int child, int parent)
    {
        int current = parent;

        for (int guard = 0; guard < LayoutSpec.TABS.length; guard++)
        {
            if (current == child)
            {
                return true;
            }

            final SnapLink link = links.get(current);
            if (link == null)
            {
                return false;
            }

            current = link.parent;
        }

        return true;
    }

    private static SnapDirection directionFor(
        Rectangle target,
        Rectangle moving
    )
    {
        final double dx =
            moving.getCenterX() - target.getCenterX();
        final double dy =
            moving.getCenterY() - target.getCenterY();

        if (Math.abs(dx) >= Math.abs(dy))
        {
            return dx < 0
                ? SnapDirection.LEFT
                : SnapDirection.RIGHT;
        }

        return dy < 0
            ? SnapDirection.ABOVE
            : SnapDirection.BELOW;
    }

    private static Point offset(
        Point parent,
        SnapDirection direction,
        int size,
        int gap
    )
    {
        switch (direction)
        {
            case LEFT:
                return new Point(
                    parent.x - size - gap,
                    parent.y
                );
            case RIGHT:
                return new Point(
                    parent.x + size + gap,
                    parent.y
                );
            case ABOVE:
                return new Point(
                    parent.x,
                    parent.y - size - gap
                );
            case BELOW:
            default:
                return new Point(
                    parent.x,
                    parent.y + size + gap
                );
        }
    }

    private static final class SnapLink
    {
        private final int parent;
        private final SnapDirection direction;

        private SnapLink(
            int parent,
            SnapDirection direction
        )
        {
            this.parent = parent;
            this.direction = direction;
        }
    }
}
