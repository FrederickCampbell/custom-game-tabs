package com.freddy.customgametabs;

import java.awt.Rectangle;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

final class LayoutSpec
{
    static final int INVENTORY_TAB = 3;
    static final int LOGOUT_TAB = 10;

    static final TabDefinition[] TABS = new TabDefinition[]
    {
        new TabDefinition("Combat Options", "CB"),
        new TabDefinition("Skills", "SK"),
        new TabDefinition("Quest List", "Q"),
        new TabDefinition("Inventory", "I"),
        new TabDefinition("Worn Equipment", "EQ"),
        new TabDefinition("Prayer", "PR"),
        new TabDefinition("Magic", "MG"),
        new TabDefinition("Friends Chat", "FC"),
        new TabDefinition("Ignore List", "IG"),
        new TabDefinition("Friends List", "FR"),
        new TabDefinition("Logout", "X"),
        new TabDefinition("Options", "OP"),
        new TabDefinition("Emotes", "EM"),
        new TabDefinition("Music Player", "MU")
    };

    static final LayoutSpec MODERN = new LayoutSpec(
        InterfaceID.TOPLEVEL_PRE_EOC,
        InterfaceID.ToplevelPreEoc.SIDE_BACKGROUND,
        InterfaceID.ToplevelPreEoc.SIDE_CONTAINER,
        new int[]
        {
            InterfaceID.ToplevelPreEoc.SIDE_STATIC_LAYER,
            InterfaceID.ToplevelPreEoc.SIDE_MOVABLE_LAYER
        },
        new int[]
        {
            InterfaceID.ToplevelPreEoc.SIDE_STATIC_BACKGROUND,
            InterfaceID.ToplevelPreEoc.SIDE_MOVABLE_BACKGROUND
        },
        new int[]
        {
            InterfaceID.ToplevelPreEoc.STONE0,
            InterfaceID.ToplevelPreEoc.STONE1,
            InterfaceID.ToplevelPreEoc.STONE2,
            InterfaceID.ToplevelPreEoc.STONE3,
            InterfaceID.ToplevelPreEoc.STONE4,
            InterfaceID.ToplevelPreEoc.STONE5,
            InterfaceID.ToplevelPreEoc.STONE6,
            InterfaceID.ToplevelPreEoc.STONE7,
            InterfaceID.ToplevelPreEoc.STONE8,
            InterfaceID.ToplevelPreEoc.STONE9,
            InterfaceID.ToplevelPreEoc.STONE10,
            InterfaceID.ToplevelPreEoc.STONE11,
            InterfaceID.ToplevelPreEoc.STONE12,
            InterfaceID.ToplevelPreEoc.STONE13
        },
        new int[]
        {
            InterfaceID.ToplevelPreEoc.ICON0,
            InterfaceID.ToplevelPreEoc.ICON1,
            InterfaceID.ToplevelPreEoc.ICON2,
            InterfaceID.ToplevelPreEoc.ICON3,
            InterfaceID.ToplevelPreEoc.ICON4,
            InterfaceID.ToplevelPreEoc.ICON5,
            InterfaceID.ToplevelPreEoc.ICON6,
            InterfaceID.ToplevelPreEoc.ICON7,
            InterfaceID.ToplevelPreEoc.ICON8,
            InterfaceID.ToplevelPreEoc.ICON9,
            InterfaceID.ToplevelPreEoc.ICON10,
            InterfaceID.ToplevelPreEoc.ICON11,
            InterfaceID.ToplevelPreEoc.ICON12,
            InterfaceID.ToplevelPreEoc.ICON13
        }
    );

    private final int rootId;
    private final int sideBackgroundId;
    private final int sideContainerId;
    private final int[] railLayerIds;
    private final int[] backgroundIds;
    private final int[] stoneIds;
    private final int[] iconIds;

    private LayoutSpec(
        int rootId,
        int sideBackgroundId,
        int sideContainerId,
        int[] railLayerIds,
        int[] backgroundIds,
        int[] stoneIds,
        int[] iconIds
    )
    {
        this.rootId = rootId;
        this.sideBackgroundId = sideBackgroundId;
        this.sideContainerId = sideContainerId;
        this.railLayerIds = railLayerIds;
        this.backgroundIds = backgroundIds;
        this.stoneIds = stoneIds;
        this.iconIds = iconIds;
    }

    int[] getRailLayerIds()
    {
        return railLayerIds;
    }

    int[] getBackgroundIds()
    {
        return backgroundIds;
    }

    int getStoneId(int tabIndex)
    {
        return stoneIds[tabIndex];
    }

    int getIconId(int tabIndex)
    {
        return iconIds[tabIndex];
    }

    boolean isSidePanelOpen(Client client)
    {
        return visibleBounds(client.getWidget(sideContainerId)) != null
            || visibleBounds(client.getWidget(sideBackgroundId)) != null;
    }

    /**
     * Capture the native side-panel rectangles on the RuneLite client thread.
     * Callers may safely publish/read the returned immutable Rectangle copies
     * from AWT mouse callbacks without touching Widget state there.
     */
    Rectangle[] captureSidePanelBounds(Client client)
    {
        final Rectangle container = visibleBounds(
            client.getWidget(sideContainerId)
        );
        final Rectangle background = visibleBounds(
            client.getWidget(sideBackgroundId)
        );

        if (container == null && background == null)
        {
            return new Rectangle[0];
        }

        if (container == null)
        {
            return new Rectangle[] { new Rectangle(background) };
        }

        if (background == null)
        {
            return new Rectangle[] { new Rectangle(container) };
        }

        return new Rectangle[]
        {
            new Rectangle(container),
            new Rectangle(background)
        };
    }

    private static Rectangle visibleBounds(Widget widget)
    {
        if (
            widget == null
                || widget.isHidden()
                || widget.getWidth() <= 0
                || widget.getHeight() <= 0
        )
        {
            return null;
        }

        final Rectangle bounds = widget.getBounds();
        return bounds == null || bounds.isEmpty() ? null : bounds;
    }

    static LayoutSpec forRoot(int rootId)
    {
        return rootId == MODERN.rootId ? MODERN : null;
    }

    static boolean isValidTab(int tabIndex)
    {
        return tabIndex >= 0 && tabIndex < TABS.length;
    }
}
