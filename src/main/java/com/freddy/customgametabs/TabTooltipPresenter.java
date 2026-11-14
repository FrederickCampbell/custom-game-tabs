package com.freddy.customgametabs;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarClientID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/**
 * Adds optional tab-name tooltips through RuneLite's shared tooltip pipeline.
 * Native game tooltips and tooltips already queued by another plugin win.
 */
@Singleton
final class TabTooltipPresenter extends Overlay
{
    private Supplier<String> textSupplier = () -> null;
    private final Client client;
    private final TooltipManager tooltipManager;

    @Inject
    TabTooltipPresenter(
        Client client,
        TooltipManager tooltipManager
    )
    {
        // Dynamic overlays sort by ascending priority, ahead of TooltipOverlay.
        // Mouse Highlight must have queued its native action before our fallback.
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGHEST);
        this.client = client;
        this.tooltipManager = tooltipManager;
    }

    void setTextSupplier(Supplier<String> textSupplier)
    {
        this.textSupplier = textSupplier;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        showIfAvailable(textSupplier.get());
        return null;
    }

    void showIfAvailable(String text)
    {
        if (
            text == null
                || text.isBlank()
                || client.isMenuOpen()
                || !tooltipManager.getTooltips().isEmpty()
        )
        {
            return;
        }

        /*
         * Match RuneLite's own Mouse Highlight tooltip etiquette: do not race a
         * native tooltip that is already visible or scheduled to appear.
         */
        if (
            client.getVarcIntValue(VarClientID.TOOLTIP_TIME)
                > client.getGameCycle()
            || client.getVarcIntValue(VarClientID.TOOLTIP_BUILT) == 1
        )
        {
            return;
        }

        tooltipManager.addFront(new Tooltip(text));
    }
}
