package com.freddy.customgametabs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class FreeformPlacementContractTest
{
    @Test
    public void absolutePresetPlacementClearsRuneLiteOriginFirst() throws IOException
    {
        final String source = Files.readString(
            Path.of(
                "src",
                "main",
                "java",
                "com",
                "freddy",
                "customgametabs",
                "CustomGameTabsPlugin.java"
            )
        );

        final int method = source.indexOf("private void placeOverlay(");
        final int reset = source.indexOf("overlayManager.resetOverlay(overlay);", method);
        final int location = source.indexOf("overlay.setPreferredLocation(", method);

        assertTrue("placeOverlay method missing", method >= 0);
        assertTrue("RuneLite overlay origin must be reset before absolute placement", reset > method);
        assertTrue("preferred location assignment missing", location > reset);
    }

    @Test
    public void freeformSnapshotsUseRuneLiteRealOverlayDimensions() throws IOException
    {
        final String source = Files.readString(
            Path.of(
                "src",
                "main",
                "java",
                "com",
                "freddy",
                "customgametabs",
                "CustomGameTabsPlugin.java"
            )
        );

        assertTrue(
            "freeform placement should use getRealDimensions() to match OverlayRenderer coordinates",
            source.contains("client.getRealDimensions()")
        );
    }

    @Test
    public void stickTogetherAndSnapShareOneCgtDropPath() throws IOException
    {
        final String source = Files.readString(
            Path.of(
                "src", "main", "java", "com", "freddy", "customgametabs",
                "CustomGameTabsPlugin.java"
            )
        );

        assertTrue(source.contains("stickyGroups = StickyGroups.detect("));
        assertTrue(source.contains("final TabSnapManager.SnapResult snap = snapManager.findSnap("));
        assertTrue(!source.contains("registerMouseListener(0,"));
    }
}
