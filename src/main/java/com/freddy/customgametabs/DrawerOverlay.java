package com.freddy.customgametabs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Drawer handle plus its temporary row/column of drawer-only tabs. */
final class DrawerOverlay extends Overlay
{
    private static final Color HANDLE_FILL = new Color(49, 38, 26, 245);
    private static final Color ARROW_GOLD = new Color(219, 166, 67);
    private static final Color ARROW_OUTLINE = new Color(79, 54, 28);
    private static final long ANIMATION_NANOS = 170_000_000L;

    private final Client client;
    private final CustomGameTabsConfig config;
    private final TabIconRenderer iconRenderer;
    private final NativeTabController nativeTabController;
    private final TabTooltipPresenter tooltipPresenter;

    private final Rectangle2D.Double[] buttonBounds =
        new Rectangle2D.Double[LayoutSpec.TABS.length];
    private volatile Rectangle2D.Double handleBounds;
    private volatile boolean rendered;
    private volatile boolean expanded;
    private volatile double animationFrom;
    private volatile long animationStartNanos;
    private volatile DrawerDirection effectiveDirection = DrawerDirection.LEFT;
    private volatile int hoveredTab = -1;
    private volatile boolean hoveredHandle;

    @Inject
    DrawerOverlay(
        Client client,
        CustomGameTabsConfig config,
        TabIconRenderer iconRenderer,
        NativeTabController nativeTabController,
        TabTooltipPresenter tooltipPresenter
    )
    {
        this.client = client;
        this.config = config;
        this.iconRenderer = iconRenderer;
        this.nativeTabController = nativeTabController;
        this.tooltipPresenter = tooltipPresenter;

        setPosition(OverlayPosition.TOP_RIGHT);
        setLayer(OverlayLayer.MANUAL);
        drawAfterLayer(InterfaceID.ToplevelPreEoc.MAP_CONTAINER);
        /* Non-dynamic overlays with lower priority draw later. Keep drawer over CGT tabs. */
        setPriority(PRIORITY_HIGH);
        setSnappable(false);
        setDragTargetable(false);
        setMinimumSize(8);
    }

    @Override
    public String getName()
    {
        return "Custom Game Tabs Drawer";
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        final LayoutSpec layout = LayoutSpec.forRoot(client.getTopLevelInterfaceId());
        final List<Integer> drawerTabs = TabLayout.drawerOrder(config);

        if (client.getGameState() != GameState.LOGGED_IN
            || layout == null
            || !config.drawerEnabled()
            || drawerTabs.isEmpty())
        {
            clearBounds();
            return null;
        }

        rendered = false;
        Arrays.fill(buttonBounds, null);
        applyQualityHints(graphics);

        final int cell = DockMetrics.buttonCellSize(config);
        final double buttonSize = DockMetrics.buttonSizeExact(config);
        final int gap = DockMetrics.gap(config);
        final double radius = DockMetrics.cornerRadiusExact(config);
        final double animation = animationProgress();

        /* Keep the handle at one stable local offset whether open or closed.
         * CGT owns input/movement, so a full transparent layout rectangle cannot
         * accidentally become a native RuneLite drag surface. */
        final Point handleOffset = localHandleOffset(
            effectiveDirection,
            true,
            drawerTabs.size(),
            cell,
            gap
        );

        final int totalWidth = contentWidth(
            effectiveDirection,
            true,
            drawerTabs.size(),
            cell,
            gap
        );
        final int totalHeight = contentHeight(
            effectiveDirection,
            true,
            drawerTabs.size(),
            cell,
            gap
        );

        final Rectangle2D.Double localHandle = new Rectangle2D.Double(
            handleOffset.x,
            handleOffset.y,
            cell,
            cell
        );

        drawHandle(graphics, localHandle, radius);
        handleBounds = new Rectangle2D.Double(
            getBounds().x + localHandle.x,
            getBounds().y + localHandle.y,
            localHandle.width,
            localHandle.height
        );

        if (animation > 0.001)
        {
            for (int position = 0; position < drawerTabs.size(); position++)
            {
                final int tabIndex = drawerTabs.get(position);
                final Point tabOffset = drawerTabOffset(
                    effectiveDirection,
                    position,
                    drawerTabs.size(),
                    cell,
                    gap
                );
                final double animatedX = handleOffset.x
                    + (tabOffset.x - handleOffset.x) * animation;
                final double animatedY = handleOffset.y
                    + (tabOffset.y - handleOffset.y) * animation;
                final Rectangle2D.Double localBounds = new Rectangle2D.Double(
                    animatedX,
                    animatedY,
                    buttonSize,
                    buttonSize
                );

                buttonBounds[tabIndex] = new Rectangle2D.Double(
                    getBounds().x + localBounds.x,
                    getBounds().y + localBounds.y,
                    localBounds.width,
                    localBounds.height
                );

                final boolean selected = nativeTabController.isTabActive(tabIndex);
                final boolean hovered = hoveredTab == tabIndex;
                final int baseOpacity = selected
                    ? config.selectedOpacity()
                    : hovered ? config.hoverOpacity() : config.idleOpacity();
                final int opacity = (int) Math.round(baseOpacity * animation);

                drawButton(
                    graphics,
                    layout,
                    tabIndex,
                    localBounds,
                    selected,
                    hovered,
                    opacity,
                    radius
                );
            }
        }

        if (config.showTooltip() && hoveredTab >= 0)
        {
            tooltipPresenter.showIfAvailable(nativeTabController.getTabName(hoveredTab));
        }

        rendered = true;
        return new Dimension(Math.max(1, totalWidth), Math.max(1, totalHeight));
    }

    void setFreeformMovable(boolean movable)
    {
        /* CGT moves the handle itself. Keeping this non-movable prevents the
         * generic OverlayRenderer from treating transparent drawer bounds as an
         * Alt-drag target. */
        setMovable(false);
    }

    void setExpanded(boolean expanded)
    {
        final double current = animationProgress();
        final double target = expanded ? 1.0 : 0.0;
        if (this.expanded == expanded && Math.abs(current - target) < 0.001)
        {
            return;
        }

        animationFrom = current;
        this.expanded = expanded;
        animationStartNanos = System.nanoTime();
        revalidate();
    }

    boolean isExpanded()
    {
        return expanded;
    }

    void setEffectiveDirection(DrawerDirection direction)
    {
        effectiveDirection = direction == null || direction == DrawerDirection.AUTO
            ? DrawerDirection.LEFT
            : direction;
        revalidate();
    }

    DrawerDirection getEffectiveDirection()
    {
        return effectiveDirection;
    }

    int tabAt(int x, int y)
    {
        if (!rendered)
        {
            return -1;
        }
        for (int tab = 0; tab < buttonBounds.length; tab++)
        {
            final Rectangle2D bounds = buttonBounds[tab];
            if (bounds != null && bounds.contains(x, y))
            {
                return tab;
            }
        }
        return -1;
    }

    boolean handleContains(int x, int y)
    {
        final Rectangle2D bounds = handleBounds;
        return rendered && bounds != null && bounds.contains(x, y);
    }

    boolean containsInputSurface(int x, int y)
    {
        if (!rendered)
        {
            return false;
        }
        return handleContains(x, y) || tabAt(x, y) >= 0;
    }

    Point getHandleLocation()
    {
        final Rectangle2D bounds = handleBounds;
        if (rendered && bounds != null)
        {
            return new Point((int) Math.round(bounds.getX()), (int) Math.round(bounds.getY()));
        }

        final Point preferred = getPreferredLocation();
        if (preferred == null)
        {
            return null;
        }
        final List<Integer> drawerTabs = TabLayout.drawerOrder(config);
        final Point offset = localHandleOffset(
            effectiveDirection,
            true,
            drawerTabs.size(),
            DockMetrics.buttonCellSize(config),
            DockMetrics.gap(config)
        );
        return new Point(preferred.x + offset.x, preferred.y + offset.y);
    }

    Point topLeftForHandle(Point handleLocation)
    {
        final List<Integer> drawerTabs = TabLayout.drawerOrder(config);
        final Point offset = localHandleOffset(
            effectiveDirection,
            true,
            drawerTabs.size(),
            DockMetrics.buttonCellSize(config),
            DockMetrics.gap(config)
        );
        return new Point(handleLocation.x - offset.x, handleLocation.y - offset.y);
    }

    Dimension contentSize()
    {
        final List<Integer> drawerTabs = TabLayout.drawerOrder(config);
        final int cell = DockMetrics.buttonCellSize(config);
        final int gap = DockMetrics.gap(config);
        return new Dimension(
            contentWidth(effectiveDirection, true, drawerTabs.size(), cell, gap),
            contentHeight(effectiveDirection, true, drawerTabs.size(), cell, gap)
        );
    }

    void onMouseMoved(int x, int y)
    {
        hoveredTab = tabAt(x, y);
        hoveredHandle = handleContains(x, y);
    }

    void onMouseExited()
    {
        hoveredTab = -1;
        hoveredHandle = false;
    }

    private void drawHandle(Graphics2D graphics, Rectangle2D.Double bounds, double radius)
    {
        final float strokeWidth = DockMetrics.borderWidth(config);
        final double inset = strokeWidth / 2.0;
        final RoundRectangle2D.Double shape = new RoundRectangle2D.Double(
            bounds.x + inset,
            bounds.y + inset,
            Math.max(0.5, bounds.width - strokeWidth),
            Math.max(0.5, bounds.height - strokeWidth),
            radius,
            radius
        );

        final int opacity = hoveredHandle ? config.hoverOpacity() : Math.max(config.idleOpacity(), 70);
        graphics.setColor(withOpacity(hoveredHandle ? config.hoverColor() : HANDLE_FILL, opacity));
        graphics.fill(shape);
        graphics.setColor(withOpacity(config.borderColor(), opacity));
        graphics.setStroke(new BasicStroke(strokeWidth));
        graphics.draw(shape);

        final int cx = (int) Math.round(bounds.getCenterX());
        final int cy = (int) Math.round(bounds.getCenterY());
        final int arm = Math.max(3, (int) Math.round(bounds.width * 0.16));
        final DrawerDirection arrowDirection = expanded
            ? opposite(effectiveDirection)
            : effectiveDirection;
        final Polygon arrow = arrowPolygon(arrowDirection, cx, cy, arm);

        graphics.setColor(ARROW_OUTLINE);
        graphics.setStroke(new BasicStroke(Math.max(2f, strokeWidth + 1f)));
        graphics.drawPolygon(arrow);
        graphics.setColor(ARROW_GOLD);
        graphics.fillPolygon(arrow);
    }

    private void drawButton(
        Graphics2D graphics,
        LayoutSpec layout,
        int tabIndex,
        Rectangle2D.Double bounds,
        boolean selected,
        boolean hovered,
        int opacity,
        double radius
    )
    {
        final Color fill = selected
            ? config.selectedColor()
            : hovered ? config.hoverColor() : config.buttonColor();
        final float strokeWidth = DockMetrics.borderWidth(config);
        final double inset = strokeWidth / 2.0;
        final RoundRectangle2D.Double shape = new RoundRectangle2D.Double(
            bounds.x + inset,
            bounds.y + inset,
            Math.max(0.5, bounds.width - strokeWidth),
            Math.max(0.5, bounds.height - strokeWidth),
            radius,
            radius
        );

        graphics.setColor(withOpacity(fill, opacity));
        graphics.fill(shape);
        graphics.setColor(withOpacity(selected ? config.borderColor().brighter() : config.borderColor(), opacity));
        graphics.setStroke(new BasicStroke(strokeWidth));
        graphics.draw(shape);

        final Widget iconWidget = client.getWidget(layout.getIconId(tabIndex));
        iconRenderer.draw(
            graphics,
            iconWidget,
            LayoutSpec.TABS[tabIndex],
            shape.getBounds2D(),
            opacity
        );
    }

    private double animationProgress()
    {
        final double target = expanded ? 1.0 : 0.0;
        final long started = animationStartNanos;
        if (started == 0L)
        {
            return target;
        }

        final double elapsed = Math.max(0.0, System.nanoTime() - started);
        final double linear = Math.min(1.0, elapsed / ANIMATION_NANOS);
        if (linear >= 1.0)
        {
            return target;
        }

        /* Cubic ease-out: responsive at the click, gentle at the final slot. */
        final double eased = 1.0 - Math.pow(1.0 - linear, 3.0);
        return animationFrom + (target - animationFrom) * eased;
    }

    private static DrawerDirection opposite(DrawerDirection direction)
    {
        switch (direction)
        {
            case LEFT: return DrawerDirection.RIGHT;
            case RIGHT: return DrawerDirection.LEFT;
            case UP: return DrawerDirection.DOWN;
            case DOWN: return DrawerDirection.UP;
            default: return DrawerDirection.LEFT;
        }
    }

    private static Polygon arrowPolygon(DrawerDirection direction, int cx, int cy, int arm)
    {
        switch (direction)
        {
            case RIGHT:
                return new Polygon(
                    new int[] {cx - arm, cx - arm, cx + arm},
                    new int[] {cy - arm, cy + arm, cy},
                    3
                );
            case UP:
                return new Polygon(
                    new int[] {cx - arm, cx + arm, cx},
                    new int[] {cy + arm, cy + arm, cy - arm},
                    3
                );
            case DOWN:
                return new Polygon(
                    new int[] {cx - arm, cx + arm, cx},
                    new int[] {cy - arm, cy - arm, cy + arm},
                    3
                );
            case LEFT:
            default:
                return new Polygon(
                    new int[] {cx + arm, cx + arm, cx - arm},
                    new int[] {cy - arm, cy + arm, cy},
                    3
                );
        }
    }

    private static Point localHandleOffset(
        DrawerDirection direction,
        boolean expanded,
        int count,
        int cell,
        int gap
    )
    {
        if (!expanded || count <= 0)
        {
            return new Point(0, 0);
        }
        final int extent = count * (cell + gap);
        switch (direction)
        {
            case LEFT: return new Point(extent, 0);
            case UP: return new Point(0, extent);
            default: return new Point(0, 0);
        }
    }

    private static Point drawerTabOffset(
        DrawerDirection direction,
        int position,
        int count,
        int cell,
        int gap
    )
    {
        final int step = cell + gap;
        switch (direction)
        {
            case LEFT:
                return new Point((count - 1 - position) * step, 0);
            case RIGHT:
                return new Point((position + 1) * step, 0);
            case UP:
                return new Point(0, (count - 1 - position) * step);
            case DOWN:
            default:
                return new Point(0, (position + 1) * step);
        }
    }

    private static int contentWidth(
        DrawerDirection direction,
        boolean expanded,
        int count,
        int cell,
        int gap
    )
    {
        if (expanded && count > 0 && (direction == DrawerDirection.LEFT || direction == DrawerDirection.RIGHT))
        {
            return (count + 1) * cell + count * gap;
        }
        return cell;
    }

    private static int contentHeight(
        DrawerDirection direction,
        boolean expanded,
        int count,
        int cell,
        int gap
    )
    {
        if (expanded && count > 0 && (direction == DrawerDirection.UP || direction == DrawerDirection.DOWN))
        {
            return (count + 1) * cell + count * gap;
        }
        return cell;
    }

    private static void applyQualityHints(Graphics2D graphics)
    {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    }

    private static Color withOpacity(Color color, int percent)
    {
        final int alpha = Math.round(color.getAlpha() * Math.max(0, Math.min(100, percent)) / 100.0f);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private void clearBounds()
    {
        rendered = false;
        handleBounds = null;
        Arrays.fill(buttonBounds, null);
        hoveredTab = -1;
        hoveredHandle = false;
    }
}
