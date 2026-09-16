package com.freddy.customgametabs;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * One-shot magnetic placement for loose buttons.
 *
 * There is intentionally no persistent relationship graph. RuneLite's own
 * overlay position is the single source of truth. Dropping A onto B merely
 * places A beside B and saves A's resulting overlay position; no hidden parent,
 * child, row, column, or historical snap relationship survives afterward.
 */
@Singleton
final class TabSnapManager
{
    private final CustomGameTabsConfig config;
    private final OverlayManager overlayManager;

    @Inject
    TabSnapManager(
        CustomGameTabsConfig config,
        OverlayManager overlayManager
    )
    {
        this.config = config;
        this.overlayManager = overlayManager;
    }

    static final class SnapResult
    {
        final int targetTab;
        final Point location;

        private SnapResult(int targetTab, Point location)
        {
            this.targetTab = targetTab;
            this.location = new Point(location);
        }
    }

    /**
     * Resolve a one-shot snap for CGT-owned Freeform dragging. The proposed
     * moving rectangle may belong to a larger sticky group; callers translate
     * that whole group by the returned location delta.
     */
    SnapResult findSnap(
        int movingTab,
        Point proposedLocation,
        int cellSize,
        List<CustomGameTabOverlay> overlays,
        Collection<Integer> excludedTabs
    )
    {
        if (!config.snapLooseButtons() || proposedLocation == null)
        {
            return null;
        }

        final int size = Math.max(1, cellSize);
        final Rectangle movingBounds = new Rectangle(
            proposedLocation.x,
            proposedLocation.y,
            size,
            size
        );
        final int capture = Math.max(6, Math.min(14, DockMetrics.gap(config) + 8));
        SnapResult best = null;
        double bestDistance = Double.MAX_VALUE;

        for (CustomGameTabOverlay target : overlays)
        {
            if (target == null
                || target.getTabIndex() == movingTab
                || (excludedTabs != null && excludedTabs.contains(target.getTabIndex())))
            {
                continue;
            }

            final Rectangle targetBounds = target.getBounds();
            if (targetBounds == null || targetBounds.isEmpty())
            {
                continue;
            }

            final Rectangle captureBounds = new Rectangle(targetBounds);
            captureBounds.grow(capture, capture);
            if (!captureBounds.intersects(movingBounds))
            {
                continue;
            }

            final double dx = movingBounds.getCenterX() - targetBounds.getCenterX();
            final double dy = movingBounds.getCenterY() - targetBounds.getCenterY();
            final double distance = dx * dx + dy * dy;
            if (distance >= bestDistance)
            {
                continue;
            }

            final SnapDirection direction = directionFor(targetBounds, movingBounds);
            final Point desired = adjacentLocation(
                targetBounds,
                movingBounds,
                direction,
                DockMetrics.gap(config)
            );
            best = new SnapResult(target.getTabIndex(), desired);
            bestDistance = distance;
        }

        return best;
    }

    boolean snap(
        CustomGameTabOverlay target,
        CustomGameTabOverlay moving
    )
    {
        if (!config.snapLooseButtons() || target == moving)
        {
            return false;
        }

        final Rectangle targetBounds = target.getBounds();
        final Rectangle movingBounds = moving.getBounds();
        final Point preferred = moving.getPreferredLocation();

        /*
         * While RuneLite is actively Alt-dragging a detached overlay it has a
         * preferred location. If that invariant is ever false, decline the
         * custom snap and let RuneLite finish/save the drag normally.
         */
        if (preferred == null
            || targetBounds.isEmpty()
            || movingBounds.isEmpty())
        {
            return false;
        }

        final SnapDirection direction = directionFor(
            targetBounds,
            movingBounds
        );
        final int gap = DockMetrics.gap(config);
        final Point desired = adjacentLocation(
            targetBounds,
            movingBounds,
            direction,
            gap
        );

        final Point adjusted = new Point(preferred);
        adjusted.translate(
            desired.x - movingBounds.x,
            desired.y - movingBounds.y
        );

        moving.setPendingSnapAbsolute(desired);
        moving.setPreferredPosition(null);
        moving.setPreferredLocation(adjusted);
        moving.revalidate();
        overlayManager.saveOverlay(moving);
        return true;
    }

    private static SnapDirection directionFor(
        Rectangle target,
        Rectangle moving
    )
    {
        final double dx = moving.getCenterX() - target.getCenterX();
        final double dy = moving.getCenterY() - target.getCenterY();

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

    private static Point adjacentLocation(
        Rectangle target,
        Rectangle moving,
        SnapDirection direction,
        int gap
    )
    {
        switch (direction)
        {
            case LEFT:
                return new Point(
                    target.x - moving.width - gap,
                    target.y
                );
            case RIGHT:
                return new Point(
                    target.x + target.width + gap,
                    target.y
                );
            case ABOVE:
                return new Point(
                    target.x,
                    target.y - moving.height - gap
                );
            case BELOW:
            default:
                return new Point(
                    target.x,
                    target.y + target.height + gap
                );
        }
    }
}
