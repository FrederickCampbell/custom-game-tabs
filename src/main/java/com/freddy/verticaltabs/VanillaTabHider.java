package com.freddy.verticaltabs;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

final class VanillaTabHider
{
    private final Client client;
    private final VerticalTabsConfig config;
    private final Map<Integer, WidgetState> originalStates =
        new LinkedHashMap<>();
    private LayoutSpec hiddenLayout;

    @Inject
    VanillaTabHider(Client client, VerticalTabsConfig config)
    {
        this.client = client;
        this.config = config;
    }

    void update()
    {
        final LayoutSpec active = LayoutSpec.forRoot(
            client.getTopLevelInterfaceId()
        );

        if (!config.hideVanillaRail() || active == null)
        {
            restoreAll();
            return;
        }

        if (hiddenLayout != active)
        {
            restoreAll();
            hiddenLayout = active;
        }

        /*
         * RuneLite exposes the two native Modern tab rails themselves as
         * movable WidgetOverlays. Hiding only their stone/icon children leaves
         * these parent layer rectangles alive, so an invisible moved rail can
         * still swallow clicks over the minimap, chatbox, or game world.
         */
        for (int id : active.getRailLayerIds())
        {
            neutralize(id, true);
        }

        for (int id : active.getBackgroundIds())
        {
            neutralize(id, false);
        }

        for (int index = 0; index < LayoutSpec.TABS.length; index++)
        {
            neutralize(active.getStoneId(index), false);
            neutralize(active.getIconId(index), false);
        }
    }

    void restoreAll()
    {
        for (Map.Entry<Integer, WidgetState> entry : originalStates.entrySet())
        {
            final Widget widget = client.getWidget(entry.getKey());
            if (widget != null)
            {
                entry.getValue().restore(widget);
            }
        }

        originalStates.clear();
        hiddenLayout = null;
    }

    private void neutralize(int componentId, boolean disableListeners)
    {
        final Widget widget = client.getWidget(componentId);
        if (widget == null)
        {
            return;
        }

        originalStates.computeIfAbsent(
            componentId,
            ignored -> WidgetState.capture(widget)
        );

        /*
         * Clear only interaction properties that could leave invisible click
         * blockers. Stone OnOp listeners remain intact so a physical click on a
         * custom tab can reuse the corresponding native top-level tab listener.
         */
        if (widget.getClickMask() != 0)
        {
            widget.setClickMask(0);
        }
        if (widget.getNoClickThrough())
        {
            widget.setNoClickThrough(false);
        }
        if (widget.getNoScrollThrough())
        {
            widget.setNoScrollThrough(false);
        }
        if (disableListeners && widget.hasListener())
        {
            widget.setHasListener(false);
        }
        if (!widget.isSelfHidden())
        {
            widget.setHidden(true);
        }
    }

    private static final class WidgetState
    {
        private final boolean hidden;
        private final int clickMask;
        private final boolean noClickThrough;
        private final boolean noScrollThrough;
        private final boolean hasListener;

        private WidgetState(
            boolean hidden,
            int clickMask,
            boolean noClickThrough,
            boolean noScrollThrough,
            boolean hasListener
        )
        {
            this.hidden = hidden;
            this.clickMask = clickMask;
            this.noClickThrough = noClickThrough;
            this.noScrollThrough = noScrollThrough;
            this.hasListener = hasListener;
        }

        private static WidgetState capture(Widget widget)
        {
            return new WidgetState(
                widget.isSelfHidden(),
                widget.getClickMask(),
                widget.getNoClickThrough(),
                widget.getNoScrollThrough(),
                widget.hasListener()
            );
        }

        private void restore(Widget widget)
        {
            if (widget.getClickMask() != clickMask)
            {
                widget.setClickMask(clickMask);
            }
            if (widget.getNoClickThrough() != noClickThrough)
            {
                widget.setNoClickThrough(noClickThrough);
            }
            if (widget.getNoScrollThrough() != noScrollThrough)
            {
                widget.setNoScrollThrough(noScrollThrough);
            }
            if (widget.hasListener() != hasListener)
            {
                widget.setHasListener(hasListener);
            }
            if (widget.isSelfHidden() != hidden)
            {
                widget.setHidden(hidden);
            }
        }
    }
}
