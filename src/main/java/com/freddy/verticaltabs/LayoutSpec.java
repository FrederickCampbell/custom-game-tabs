package com.freddy.verticaltabs;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

final class LayoutSpec
{
    static final TabDefinition[] TABS = new TabDefinition[]
    {
        new TabDefinition(0, "Combat Options", "CB"),
        new TabDefinition(1, "Skills", "SK"),
        new TabDefinition(2, "Quest List", "Q"),
        new TabDefinition(3, "Inventory", "I"),
        new TabDefinition(4, "Worn Equipment", "EQ"),
        new TabDefinition(5, "Prayer", "PR"),
        new TabDefinition(6, "Magic", "MG"),
        new TabDefinition(7, "Friends Chat", "FC"),
        new TabDefinition(8, "Ignore List", "IG"),
        new TabDefinition(9, "Friends List", "FR"),
        new TabDefinition(10, "Logout", "X"),
        new TabDefinition(11, "Options", "OP"),
        new TabDefinition(12, "Emotes", "EM"),
        new TabDefinition(13, "Music Player", "MU")
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
    private final int sidePanelProbeId;
    private final int sidePanelContainerId;
    private final int[] railLayerIds;
    private final int[] backgroundIds;
    private final int[] stoneIds;
    private final int[] iconIds;

    private LayoutSpec(
        int rootId,
        int sidePanelProbeId,
        int sidePanelContainerId,
        int[] railLayerIds,
        int[] backgroundIds,
        int[] stoneIds,
        int[] iconIds
    )
    {
        this.rootId = rootId;
        this.sidePanelProbeId = sidePanelProbeId;
        this.sidePanelContainerId = sidePanelContainerId;
        this.railLayerIds = railLayerIds;
        this.backgroundIds = backgroundIds;
        this.stoneIds = stoneIds;
        this.iconIds = iconIds;
    }

    int getRootId()
    {
        return rootId;
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

    Widget getSidePanelRoot(Client client)
    {
        final Widget container = client.getWidget(sidePanelContainerId);
        if (container != null)
        {
            return container;
        }

        final Widget probe = client.getWidget(sidePanelProbeId);
        return probe == null || probe.getParent() == null
            ? probe
            : probe.getParent();
    }

    boolean isSidePanelOpen(Client client)
    {
        final Widget panel = client.getWidget(sidePanelProbeId);
        return panel != null
            && !panel.isHidden()
            && panel.getWidth() > 0
            && panel.getHeight() > 0;
    }

    static LayoutSpec forRoot(int rootId)
    {
        return rootId == MODERN.rootId ? MODERN : null;
    }
}
