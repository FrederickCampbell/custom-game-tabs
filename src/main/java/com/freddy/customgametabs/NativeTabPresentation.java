package com.freddy.customgametabs;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

/**
 * Hides the original Resizable Modern tab presentation while leaving the real
 * widgets otherwise untouched.
 *
 * The native STONE widgets are backing authorities for Custom Game Tabs: their
 * actions, sub-ops, listeners, click configuration, and component identity are
 * intentionally preserved. Hidden state alone is enough to remove the original
 * rail from normal rendering/hit-testing without rewriting native semantics.
 */
final class NativeTabPresentation
{
    private final Client client;
    private final Map<Integer, CapturedWidgetState> originalStates =
        new LinkedHashMap<>();
    private LayoutSpec hiddenLayout;

    @Inject
    NativeTabPresentation(Client client)
    {
        this.client = client;
    }

    void update()
    {
        final LayoutSpec active = LayoutSpec.forRoot(
            client.getTopLevelInterfaceId()
        );

        if (active == null)
        {
            restoreAll();
            return;
        }

        if (hiddenLayout != active)
        {
            restoreAll();
            hiddenLayout = active;
        }

        for (int id : active.getRailLayerIds())
        {
            hide(id);
        }

        for (int id : active.getBackgroundIds())
        {
            hide(id);
        }

        /*
         * Hide the visible native controls too, but do not alter any of their
         * interaction/listener/action fields. The custom UI reads those fields
         * while the native widget remains in RuneScape's expected hierarchy.
         */
        for (int index = 0; index < LayoutSpec.TABS.length; index++)
        {
            hide(active.getStoneId(index));
            hide(active.getIconId(index));
        }
    }

    void restoreAll()
    {
        for (Map.Entry<Integer, CapturedWidgetState> entry :
            originalStates.entrySet())
        {
            final Widget current = client.getWidget(entry.getKey());
            final CapturedWidgetState captured = entry.getValue();

            /* Never restore stale state onto a replacement Widget instance. */
            if (current != null && current == captured.widget)
            {
                if (current.isSelfHidden() != captured.hidden)
                {
                    current.setHidden(captured.hidden);
                }
            }
        }

        originalStates.clear();
        hiddenLayout = null;
    }

    private void hide(int componentId)
    {
        final Widget widget = client.getWidget(componentId);
        if (widget == null)
        {
            return;
        }

        final CapturedWidgetState existing = originalStates.get(componentId);
        if (existing == null || existing.widget != widget)
        {
            originalStates.put(
                componentId,
                new CapturedWidgetState(widget, widget.isSelfHidden())
            );
        }

        if (!widget.isSelfHidden())
        {
            widget.setHidden(true);
        }
    }

    private static final class CapturedWidgetState
    {
        private final Widget widget;
        private final boolean hidden;

        private CapturedWidgetState(Widget widget, boolean hidden)
        {
            this.widget = widget;
            this.hidden = hidden;
        }
    }
}
