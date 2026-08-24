package com.freddy.verticaltabs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class IndividualTabOverlay extends Overlay
{
    private final Client client;
    private final VerticalTabsConfig config;
    private final TabIconRenderer iconRenderer;
    private final SidePanelManager sidePanelManager;
    private final LooseSnapManager snapManager;
    private final int tabIndex;

    private volatile boolean hovered;
    private volatile boolean rendered;

    IndividualTabOverlay(
        Client client,
        VerticalTabsConfig config,
        TabIconRenderer iconRenderer,
        SidePanelManager sidePanelManager,
        LooseSnapManager snapManager,
        int tabIndex
    )
    {
        this.client = client;
        this.config = config;
        this.iconRenderer = iconRenderer;
        this.sidePanelManager = sidePanelManager;
        this.snapManager = snapManager;
        this.tabIndex = tabIndex;

        setPosition(OverlayPosition.TOP_RIGHT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGHEST);
        setDragTargetable(true);
        setMinimumSize(8);
    }

    @Override
    public String getName()
    {
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
                || !config.moveSeparately()
                || !TabLayout.isShown(config, tabIndex)
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
            sidePanelManager.isTabActive(tabIndex);
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

        rendered = true;
        return new Dimension(cellSize, cellSize);
    }

    int getTabIndex()
    {
        return tabIndex;
    }

    boolean contains(int x, int y)
    {
        return rendered
            && client.getGameState() == GameState.LOGGED_IN
            && LayoutSpec.forRoot(client.getTopLevelInterfaceId()) != null
            && config.moveSeparately()
            && TabLayout.isShown(config, tabIndex)
            && getBounds().contains(x, y);
    }

    void setHovered(boolean hovered)
    {
        this.hovered = hovered;
    }

    @Override
    public boolean onDrag(Overlay other)
    {
        if (!(other instanceof IndividualTabOverlay))
        {
            return false;
        }

        return snapManager.snap(
            this,
            (IndividualTabOverlay) other
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
