package com.freddy.customgametabs;

final class DockMetrics
{
    private static final double NORMAL_BUTTON_SIZE = 42.0;
    private static final double NORMAL_FRAME_PADDING = 3.0;
    private static final double NORMAL_CORNER_RADIUS = 6.0;
    private static final float NORMAL_BORDER_WIDTH = 1.2f;

    private DockMetrics()
    {
    }

    static double buttonSizeExact(CustomGameTabsConfig config)
    {
        return scaleExact(
            NORMAL_BUTTON_SIZE,
            config.buttonScale()
        );
    }

    static int buttonCellSize(CustomGameTabsConfig config)
    {
        return Math.max(
            1,
            (int) Math.ceil(buttonSizeExact(config))
        );
    }

    static int gap(CustomGameTabsConfig config)
    {
        return gapPixels(config.gap());
    }

    static int gapPixels(int configuredPixels)
    {
        return Math.max(0, configuredPixels);
    }

    static double framePaddingExact(CustomGameTabsConfig config)
    {
        return Math.max(
            1.0,
            scaleExact(
                NORMAL_FRAME_PADDING,
                config.buttonScale()
            )
        );
    }

    static double cornerRadiusExact(CustomGameTabsConfig config)
    {
        return Math.max(
            2.0,
            scaleExact(
                NORMAL_CORNER_RADIUS,
                config.buttonScale()
            )
        );
    }

    static float borderWidth(CustomGameTabsConfig config)
    {
        return Math.max(
            0.65f,
            NORMAL_BORDER_WIDTH
                * clampScale(config.buttonScale())
                / 100.0f
        );
    }

    static double scaleExact(double normalPixels, int percent)
    {
        return Math.max(
            0.01,
            normalPixels * clampScale(percent) / 100.0
        );
    }

    private static int clampScale(int percent)
    {
        return Math.max(25, Math.min(200, percent));
    }
}
