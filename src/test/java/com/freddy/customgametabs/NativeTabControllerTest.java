package com.freddy.customgametabs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Regression guard for the native side-panel ownership policy.
 *
 * NativeTabController intentionally has no synthetic blocking/restore state to
 * unit-test anymore; RuneScape owns panel lifetime and temporary interfaces.
 */
public class NativeTabControllerTest
{
    @Test
    public void onlyRealGameTabIndexesAreValid()
    {
        assertTrue(LayoutSpec.isValidTab(0));
        assertTrue(LayoutSpec.isValidTab(LayoutSpec.INVENTORY_TAB));
        assertTrue(LayoutSpec.isValidTab(LayoutSpec.LOGOUT_TAB));
        assertTrue(LayoutSpec.isValidTab(13));
        assertFalse(LayoutSpec.isValidTab(-1));
        assertFalse(LayoutSpec.isValidTab(14));
    }
}
