package com.freddy.customgametabs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class LoadingScreenPersistenceContractTest
{
    @Test
    public void loadingTransitionDoesNotOverwriteCanonicalWorkingSnapshot() throws IOException
    {
        final String source = pluginSource();
        final int method = source.indexOf("public void onGameStateChanged(GameStateChanged event)");
        final int nextMethod = source.indexOf("@Subscribe", method + 10);
        final String body = source.substring(method, nextMethod);

        assertTrue("game-state handler missing", method >= 0);
        assertTrue(
            "loading transition must not capture transient overlay geometry",
            !body.contains("saveWorkingSnapshot()")
        );
        assertTrue(
            "logged-in transition must schedule delayed canonical restore",
            body.contains("restoreWorkingCountdown = 2")
        );
    }

    @Test
    public void delayedRestoreReappliesSavedFreeformCoordinates() throws IOException
    {
        final String source = pluginSource();
        final int method = source.indexOf("private void restoreWorkingAfterLoadScreen()");
        assertTrue("restoreWorkingAfterLoadScreen missing", method >= 0);
        assertTrue(
            "loading-screen restore must read the persisted working activity layout",
            source.indexOf("activityLayoutStore.loadWorking()", method) > method
        );
        assertTrue(
            "loading-screen restore must reapply canonical freeform positions",
            source.indexOf("applyLocations(resolveLocations(working))", method) > method
        );
    }

    private static String pluginSource() throws IOException
    {
        return Files.readString(Path.of(
            "src", "main", "java", "com", "freddy", "customgametabs", "CustomGameTabsPlugin.java"
        ));
    }
}
