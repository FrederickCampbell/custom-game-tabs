package com.freddy.customgametabs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class CustomGameTabOverlay extends Overlay
{
    private final Client client;
    private final CustomGameTabsConfig config;
    private final TabIconRenderer iconRenderer;
    private final NativeTabController nativeTabController;
    private final TabSnapManager snapManager;
    private final TabTooltipPresenter tooltipPresenter;
    private final int tabIndex;

    private volatile boolean hovered;
    private volatile boolean rendered;
    private volatile boolean customSnapEnabled = true;
    private volatile Point pendingSnapAbsolute;

    CustomGameTabOverlay(
        Client client,
        CustomGameTabsConfig config,
        TabIconRenderer iconRenderer,
        NativeTabController nativeTabController,
        TabSnapManager snapManager,
        TabTooltipPresenter tooltipPresenter,
        int tabIndex
    )
    {
        this.client = client;
        this.config = config;
        this.iconRenderer = iconRenderer;
        this.nativeTabController = nativeTabController;
        this.snapManager = snapManager;
        this.tooltipPresenter = tooltipPresenter;
        this.tabIndex = tabIndex;

        setPosition(OverlayPosition.TOP_RIGHT);
        setLayer(OverlayLayer.MANUAL);
        drawAfterLayer(InterfaceID.ToplevelPreEoc.MAP_CONTAINER);
        setPriority(PRIORITY_HIGHEST);
        setDragTargetable(true);
        /* Button-to-button snapping is ours; do not also snap to RuneLite corners. */
        setSnappable(false);
        setMinimumSize(8);
    }

    @Override
    public String getName()
    {
        /*
         * OverlayManager persists placement by overlay name. Keep this stable
         * even when RuneScape changes the current primary action label.
         */
        return "Game Tab - "
            + LayoutSpec.TABS[tabIndex].getName();
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        final LayoutSpec layout = LayoutSpec.forRoot(
            client.getTopLevelInterfaceId()
        );

        if (
            client.getGameState() != GameState.LOGGED_IN
                || layout == null
                || config.layoutMode() != TabLayoutMode.FREEFORM
                || TabLayout.state(config, tabIndex) != TabState.MAIN
        )
        {
            rendered = false;
            return null;
        }

        applyQualityHints(graphics);

        final double size =
            DockMetrics.buttonSizeExact(config);
        final int cellSize =
            DockMetrics.buttonCellSize(config);
        final double radius =
            DockMetrics.cornerRadiusExact(config);
        final float strokeWidth =
            DockMetrics.borderWidth(config);
        final double inset = strokeWidth / 2.0;
        final boolean selected =
            nativeTabController.isTabActive(tabIndex);
        final int opacity = selected
            ? config.selectedOpacity()
            : hovered
                ? config.hoverOpacity()
                : config.idleOpacity();
        final Color fill = selected
            ? config.selectedColor()
            : hovered
                ? config.hoverColor()
                : config.buttonColor();

        final RoundRectangle2D.Double shape =
            new RoundRectangle2D.Double(
                inset,
                inset,
                Math.max(0.5, size - strokeWidth),
                Math.max(0.5, size - strokeWidth),
                radius,
                radius
            );

        graphics.setColor(withOpacity(fill, opacity));
        graphics.fill(shape);
        graphics.setColor(
            withOpacity(
                selected
                    ? config.borderColor().brighter()
                    : config.borderColor(),
                opacity
            )
        );
        graphics.setStroke(new BasicStroke(strokeWidth));
        graphics.draw(shape);

        final Widget iconWidget =
            client.getWidget(layout.getIconId(tabIndex));

        iconRenderer.draw(
            graphics,
            iconWidget,
            LayoutSpec.TABS[tabIndex],
            shape.getBounds2D(),
            opacity
        );

        if (config.showTooltip() && hovered)
        {
            tooltipPresenter.showIfAvailable(
                nativeTabController.getTabName(tabIndex)
            );
        }

        rendered = true;
        return new Dimension(cellSize, cellSize);
    }

    int getTabIndex()
    {
        return tabIndex;
    }

    boolean contains(int x, int y)
    {
        /*
         * Mouse-thread hit testing uses the render snapshot only. render()
         * already sets rendered=false whenever this overlay is not valid.
         */
        return rendered && getBounds().contains(x, y);
    }

    void setHovered(boolean hovered)
    {
        this.hovered = hovered;
    }

    void setCustomSnapEnabled(boolean enabled)
    {
        customSnapEnabled = enabled;
    }

    void setPendingSnapAbsolute(Point location)
    {
        pendingSnapAbsolute = location == null ? null : new Point(location);
    }

    Point consumePendingSnapAbsolute()
    {
        final Point result = pendingSnapAbsolute;
        pendingSnapAbsolute = null;
        return result == null ? null : new Point(result);
    }

    @Override
    public boolean onDrag(Overlay other)
    {
        if (!customSnapEnabled || !(other instanceof CustomGameTabOverlay))
        {
            return false;
        }

        return snapManager.snap(
            this,
            (CustomGameTabOverlay) other
        );
    }

    private static void applyQualityHints(Graphics2D graphics)
    {
        graphics.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );
        graphics.setRenderingHint(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY
        );
        graphics.setRenderingHint(
            RenderingHints.KEY_STROKE_CONTROL,
            RenderingHints.VALUE_STROKE_PURE
        );
        graphics.setRenderingHint(
            RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
        );
    }

    private static Color withOpacity(Color color, int percent)
    {
        final int alpha = Math.round(
            color.getAlpha()
                * clampPercent(percent)
                / 100.0f
        );

        return new Color(
            color.getRed(),
            color.getGreen(),
            color.getBlue(),
            Math.max(0, Math.min(255, alpha))
        );
    }

    private static int clampPercent(int percent)
    {
        return Math.max(0, Math.min(100, percent));
    }
}
