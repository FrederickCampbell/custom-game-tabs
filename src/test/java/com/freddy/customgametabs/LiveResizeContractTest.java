package com.freddy.customgametabs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class LiveResizeContractTest
{
    @Test
    public void postClientTickDetectsLiveCanvasResize() throws IOException
    {
        final String source = pluginSource();
        final int tick = source.indexOf("public void onPostClientTick(PostClientTick event)");
        final int resizeCall = source.indexOf("handleCanvasResize();", tick);

        assertTrue("PostClientTick handler missing", tick >= 0);
        assertTrue("live canvas resize must be handled before normal geometry refresh", resizeCall > tick);
    }

    @Test
    public void resizeRestoresFromCanonicalWorkingSnapshotInsteadOfClampedLiveBounds() throws IOException
    {
        final String source = pluginSource();
        final int method = source.indexOf("private void handleCanvasResize()");
        final int next = source.indexOf("private Map<Integer, Point> resolveLocations", method);
        final String body = source.substring(method, next);

        assertTrue("handleCanvasResize missing", method >= 0);
        assertTrue(body.contains("activityLayoutStore.loadWorking()"));
        assertTrue(body.contains("working.resolveLocations("));
        assertTrue(body.contains("applyLocations(resolved);"));
        assertTrue(
            "resize path must not rebuild from independently clamped rendered bounds",
            !body.contains("currentFreeformLocations()")
        );
    }

    @Test
    public void applyingResolvedLocationsDoesNotClampTabsIndividually() throws IOException
    {
        final String source = pluginSource();
        final int method = source.indexOf("private void applyLocations(Map<Integer, Point> locations)");
        final int next = source.indexOf("private void placeOverlay", method);
        final String body = source.substring(method, next);

        assertTrue("applyLocations missing", method >= 0);
        assertTrue(body.contains("placeOverlay(overlay, location);"));
        assertTrue(
            "resolved rigid components must not be individually clamped",
            !body.contains("clampTabLocation(location)")
        );
    }

    private static String pluginSource() throws IOException
    {
        return Files.readString(Path.of(
            "src", "main", "java", "com", "freddy", "customgametabs", "CustomGameTabsPlugin.java"
        ));
    }
}
