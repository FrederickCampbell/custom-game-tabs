package com.freddy.verticaltabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SidePanelStateTest
{
    @Test
    public void normalOpenTabsFollowTopLevelPanel()
    {
        assertEquals(
            LayoutSpec.INVENTORY_TAB,
            SidePanelManager.resolveDisplayedTab(
                LayoutSpec.INVENTORY_TAB,
                true,
                false,
                false
            )
        );
        assertEquals(
            5,
            SidePanelManager.resolveDisplayedTab(5, true, false, false)
        );
    }

    @Test
    public void closedNormalPanelHasNoFalseHighlight()
    {
        assertEquals(
            -1,
            SidePanelManager.resolveDisplayedTab(5, false, false, false)
        );
    }

    @Test
    public void bankStyleOverrideShowsInventoryRegardlessOfStaleSelectedTab()
    {
        assertEquals(
            LayoutSpec.INVENTORY_TAB,
            SidePanelManager.resolveDisplayedTab(5, true, true, true)
        );
    }

    @Test
    public void nonInventoryBlockingInterfaceShowsNoFalseGameTab()
    {
        assertEquals(
            -1,
            SidePanelManager.resolveDisplayedTab(5, false, true, false)
        );
    }

    @Test
    public void logoutAlwaysWins()
    {
        assertEquals(
            LayoutSpec.LOGOUT_TAB,
            SidePanelManager.resolveDisplayedTab(
                LayoutSpec.LOGOUT_TAB,
                false,
                true,
                true
            )
        );
    }

    @Test
    public void blockingRuleAppliesOnlyToCustomClicksAndNeverLogout()
    {
        assertTrue(
            SidePanelManager.shouldBlockCustomActivation(true, 5)
        );
        assertFalse(
            SidePanelManager.shouldBlockCustomActivation(
                true,
                LayoutSpec.LOGOUT_TAB
            )
        );
        assertFalse(
            SidePanelManager.shouldBlockCustomActivation(false, 5)
        );
    }
}
