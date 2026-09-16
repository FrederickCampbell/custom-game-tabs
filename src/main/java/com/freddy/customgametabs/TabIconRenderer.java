package com.freddy.customgametabs;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.SpriteManager;

@Singleton
final class TabIconRenderer
{
    private static final Color FALLBACK_TEXT =
        new Color(238, 216, 158);

    private final SpriteManager spriteManager;
    private final Map<Integer, BufferedImage> trimmedSprites =
        new HashMap<>();

    @Inject
    TabIconRenderer(SpriteManager spriteManager)
    {
        this.spriteManager = spriteManager;
    }

    void draw(
        Graphics2D graphics,
        Widget iconWidget,
        TabDefinition tab,
        Rectangle2D bounds,
        int opacity
    )
    {
        BufferedImage image = null;

        if (iconWidget != null && iconWidget.getSpriteId() >= 0)
        {
            final int spriteId = iconWidget.getSpriteId();
            image = trimmedSprites.computeIfAbsent(
                spriteId,
                this::loadAndTrim
            );
        }

        if (image == null)
        {
            drawFallback(graphics, tab, bounds, opacity);
            return;
        }

        /*
         * Keep the original sprite crisp while allowing true subpixel
         * translation and scaling inside the integer RuneLite overlay bounds.
         */
        final double padding = Math.max(
            0.45,
            Math.min(bounds.getWidth(), bounds.getHeight()) * 0.035
        );
        final double availableWidth = Math.max(
            0.5,
            bounds.getWidth() - padding * 2.0
        );
        final double availableHeight = Math.max(
            0.5,
            bounds.getHeight() - padding * 2.0
        );
        final double scale = Math.min(
            availableWidth / image.getWidth(),
            availableHeight / image.getHeight()
        );
        final double width = image.getWidth() * scale;
        final double height = image.getHeight() * scale;
        final double x =
            bounds.getX() + (bounds.getWidth() - width) / 2.0;
        final double y =
            bounds.getY() + (bounds.getHeight() - height) / 2.0;

        final Graphics2D quality =
            (Graphics2D) graphics.create();

        try
        {
            quality.setComposite(
                AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER,
                    clampPercent(opacity) / 100.0f
                )
            );
            quality.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
            );
            quality.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            quality.setRenderingHint(
                RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
            );

            final AffineTransform transform =
                AffineTransform.getTranslateInstance(x, y);
            transform.scale(
                width / image.getWidth(),
                height / image.getHeight()
            );

            quality.drawImage(image, transform, null);
        }
        finally
        {
            quality.dispose();
        }
    }

    private BufferedImage loadAndTrim(int spriteId)
    {
        final BufferedImage source =
            spriteManager.getSprite(spriteId, 0);

        return source == null ? null : trimTransparent(source);
    }

    private static BufferedImage trimTransparent(BufferedImage source)
    {
        int left = source.getWidth();
        int top = source.getHeight();
        int right = -1;
        int bottom = -1;

        for (int y = 0; y < source.getHeight(); y++)
        {
            for (int x = 0; x < source.getWidth(); x++)
            {
                final int alpha = source.getRGB(x, y) >>> 24;

                if (alpha > 8)
                {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }

        if (right < left || bottom < top)
        {
            return source;
        }

        return source.getSubimage(
            left,
            top,
            right - left + 1,
            bottom - top + 1
        );
    }

    private static void drawFallback(
        Graphics2D graphics,
        TabDefinition tab,
        Rectangle2D bounds,
        int opacity
    )
    {
        final Graphics2D quality =
            (Graphics2D) graphics.create();

        try
        {
            final Font oldFont = quality.getFont();
            final float fontSize = Math.max(
                6.0f,
                (float) bounds.getHeight() * 0.30f
            );

            quality.setFont(oldFont.deriveFont(fontSize));
            quality.setColor(
                withOpacity(FALLBACK_TEXT, opacity)
            );
            quality.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );

            final FontMetrics metrics = quality.getFontMetrics();
            final String text = tab.getFallback();
            final float x = (float) (
                bounds.getX()
                    + (bounds.getWidth()
                    - metrics.stringWidth(text)) / 2.0
            );
            final float y = (float) (
                bounds.getY()
                    + (bounds.getHeight()
                    - metrics.getHeight()) / 2.0
                    + metrics.getAscent()
            );

            quality.drawString(text, x, y);
        }
        finally
        {
            quality.dispose();
        }
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
