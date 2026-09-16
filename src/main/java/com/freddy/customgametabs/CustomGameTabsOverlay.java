package com.freddy.customgametabs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class CustomGameTabsOverlay extends Overlay
{
    private static final Color FRAME_COLOR =
        new Color(28, 21, 15, 255);

    private final Client client;
    private final CustomGameTabsConfig config;
    private final TabIconRenderer iconRenderer;
    private final NativeTabController nativeTabController;
    private final TabTooltipPresenter tooltipPresenter;
    private final Rectangle2D.Double[] buttonBounds =
        new Rectangle2D.Double[LayoutSpec.TABS.length];

    private volatile int hoveredTab = -1;
    private volatile boolean rendered;

    @Inject
    CustomGameTabsOverlay(
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
        setPriority(PRIORITY_HIGHEST);
        setMinimumSize(8);
    }

    @Override
    public String getName()
    {
        return "Custom Game Tabs";
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
                || config.layoutMode() != TabLayoutMode.DOCKED
        )
        {
            clearBounds();
            return null;
        }

        final List<Integer> order =
            TabLayout.mainOrder(config);

        if (order.isEmpty())
        {
            clearBounds();
            return null;
        }

        rendered = false;
        Arrays.fill(buttonBounds, null);
        applyQualityHints(graphics);

        final double buttonSize =
            DockMetrics.buttonSizeExact(config);
        final int gap = DockMetrics.gap(config);
        final double padding =
            DockMetrics.framePaddingExact(config);
        final double radius =
            DockMetrics.cornerRadiusExact(config);
        final int columns = Math.max(
            1,
            Math.min(config.buttonsPerRow(), order.size())
        );
        final int rows =
            (order.size() + columns - 1) / columns;
        final double railWidth =
            columns * buttonSize
                + Math.max(0, columns - 1) * gap;
        final double railHeight =
            rows * buttonSize
                + Math.max(0, rows - 1) * gap;
        final double totalWidth =
            railWidth + padding * 2.0;
        final double totalHeight =
            railHeight + padding * 2.0;

        drawFrame(
            graphics,
            totalWidth,
            totalHeight,
            radius
        );

        for (int position = 0; position < order.size(); position++)
        {
            final int tabIndex = order.get(position);
            final int row = position / columns;
            final int column = position % columns;
            final double x =
                padding + column * (buttonSize + gap);
            final double y =
                padding + row * (buttonSize + gap);
            final Rectangle2D.Double localBounds =
                new Rectangle2D.Double(
                    x,
                    y,
                    buttonSize,
                    buttonSize
                );

            buttonBounds[tabIndex] = new Rectangle2D.Double(
                getBounds().x + x,
                getBounds().y + y,
                buttonSize,
                buttonSize
            );

            final boolean selected =
                nativeTabController.isTabActive(tabIndex);
            final boolean hovered =
                hoveredTab == tabIndex;
            final int opacity = selected
                ? config.selectedOpacity()
                : hovered
                    ? config.hoverOpacity()
                    : config.idleOpacity();

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

        if (
            config.showTooltip()
                && hoveredTab >= 0
                && hoveredTab < LayoutSpec.TABS.length
        )
        {
            tooltipPresenter.showIfAvailable(
                nativeTabController.getTabName(hoveredTab)
            );
        }

        rendered = true;
        return new Dimension(
            Math.max(1, (int) Math.ceil(totalWidth)),
            Math.max(1, (int) Math.ceil(totalHeight))
        );
    }

    void onMouseMoved(int x, int y)
    {
        hoveredTab = tabAt(x, y);
    }

    void onMouseExited()
    {
        hoveredTab = -1;
    }

    int tabAt(int x, int y)
    {
        /*
         * This is called by AWT mouse handlers. render() owns all RuneLite
         * client/widget reads and publishes the current hit-test snapshot.
         */
        if (!rendered)
        {
            return -1;
        }

        for (int index = 0; index < buttonBounds.length; index++)
        {
            final Rectangle2D bounds = buttonBounds[index];

            if (bounds != null && bounds.contains(x, y))
            {
                return index;
            }
        }

        return -1;
    }

    boolean containsInputSurface(int x, int y)
    {
        if (!rendered)
        {
            return false;
        }

        if (config.frameOpacity() <= 0)
        {
            return tabAt(x, y) >= 0;
        }

        return getBounds().contains(x, y);
    }

    Map<Integer, Point> captureButtonLocations()
    {
        final Map<Integer, Point> result = new LinkedHashMap<>();

        /*
         * rendered is volatile and is written after buttonBounds, so a true
         * read publishes the complete geometry snapshot to non-render threads.
         */
        if (!rendered)
        {
            return result;
        }

        for (int index = 0; index < buttonBounds.length; index++)
        {
            final Rectangle2D.Double bounds = buttonBounds[index];
            if (bounds != null)
            {
                result.put(
                    index,
                    new Point(
                        (int) Math.round(bounds.x),
                        (int) Math.round(bounds.y)
                    )
                );
            }
        }

        return result;
    }

    Map<Integer, Point> projectButtonLocations()
    {
        final List<Integer> order = TabLayout.mainOrder(config);
        final Map<Integer, Point> result = new LinkedHashMap<>();
        if (order.isEmpty())
        {
            return result;
        }

        final double buttonSize = DockMetrics.buttonSizeExact(config);
        final int gap = DockMetrics.gap(config);
        final double padding = DockMetrics.framePaddingExact(config);
        final int columns = Math.max(
            1,
            Math.min(config.buttonsPerRow(), order.size())
        );
        final int projectedWidth = Math.max(
            1,
            (int) Math.ceil(
                columns * buttonSize
                    + Math.max(0, columns - 1) * gap
                    + padding * 2.0
            )
        );

        final java.awt.Rectangle bounds = getBounds();
        final Point preferred = getPreferredLocation();
        final int originX;
        final int originY;

        if (bounds != null && !bounds.isEmpty())
        {
            originX = bounds.x;
            originY = bounds.y;
        }
        else if (preferred != null)
        {
            originX = preferred.x;
            originY = preferred.y;
        }
        else
        {
            originX = Math.max(
                0,
                client.getRealDimensions().width - projectedWidth
            );
            originY = 0;
        }

        for (int position = 0; position < order.size(); position++)
        {
            final int tabIndex = order.get(position);
            final int row = position / columns;
            final int column = position % columns;
            result.put(
                tabIndex,
                new Point(
                    (int) Math.round(
                        originX + padding + column * (buttonSize + gap)
                    ),
                    (int) Math.round(
                        originY + padding + row * (buttonSize + gap)
                    )
                )
            );
        }

        return result;
    }

    void configurationChanged()
    {
        hoveredTab = -1;
        revalidate();
    }

    private void drawFrame(
        Graphics2D graphics,
        double width,
        double height,
        double radius
    )
    {
        final int opacity = config.frameOpacity();
        final float strokeWidth =
            DockMetrics.borderWidth(config);
        final double inset = strokeWidth / 2.0;

        final RoundRectangle2D.Double frame =
            new RoundRectangle2D.Double(
                inset,
                inset,
                Math.max(0.5, width - strokeWidth),
                Math.max(0.5, height - strokeWidth),
                radius + 2.0,
                radius + 2.0
            );

        graphics.setColor(
            withOpacity(FRAME_COLOR, opacity)
        );
        graphics.fill(frame);
        graphics.setColor(
            withOpacity(config.borderColor(), opacity)
        );
        graphics.setStroke(new BasicStroke(strokeWidth));
        graphics.draw(frame);
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
            : hovered
                ? config.hoverColor()
                : config.buttonColor();
        final float strokeWidth =
            DockMetrics.borderWidth(config);
        final double inset = strokeWidth / 2.0;

        final RoundRectangle2D.Double shape =
            new RoundRectangle2D.Double(
                bounds.x + inset,
                bounds.y + inset,
                Math.max(0.5, bounds.width - strokeWidth),
                Math.max(0.5, bounds.height - strokeWidth),
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

    private void clearBounds()
    {
        rendered = false;
        Arrays.fill(buttonBounds, null);
        hoveredTab = -1;
    }
}
