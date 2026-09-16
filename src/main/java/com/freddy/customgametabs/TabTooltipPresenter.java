package com.freddy.customgametabs;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarClientID;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/**
 * Adds optional tab-name tooltips through RuneLite's shared tooltip pipeline.
 * Native game tooltips and tooltips already queued by another plugin win.
 */
@Singleton
final class TabTooltipPresenter
{
    private final Client client;
    private final TooltipManager tooltipManager;

    @Inject
    TabTooltipPresenter(
        Client client,
        TooltipManager tooltipManager
    )
    {
        this.client = client;
        this.tooltipManager = tooltipManager;
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
