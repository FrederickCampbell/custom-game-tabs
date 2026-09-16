package com.freddy.customgametabs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ActivityAppearancePresetContractTest
{
    @Test
    public void captureIncludesAppearanceAndLoadRestoresIt() throws IOException
    {
        final String source = Files.readString(Path.of(
            "src", "main", "java", "com", "freddy", "customgametabs", "CustomGameTabsPlugin.java"
        ));

        final int capture = source.indexOf("private ActivityLayoutSnapshot captureCurrentActivity()");
        final int recovery = source.indexOf("private void saveRecoverySnapshot()", capture);
        final String captureBody = source.substring(capture, recovery);
        assertTrue(captureBody.contains("new ActivityLayoutSnapshot.Appearance("));
        assertTrue(captureBody.contains("config.buttonScale()"));
        assertTrue(captureBody.contains("config.gap()"));
        assertTrue(captureBody.contains("config.buttonColor()"));
        assertTrue(captureBody.contains("config.borderColor()"));

        final int applySnapshot = source.indexOf("private void applyActivitySnapshot(ActivityLayoutSnapshot snapshot)");
        final int applySettingsCall = source.indexOf("applyActivitySettings(snapshot);", applySnapshot);
        final int resolveLocationsCall = source.indexOf("resolveLocations(snapshot)", applySnapshot);
        assertTrue("saved appearance must be applied before geometry is resolved",
            applySettingsCall >= 0 && resolveLocationsCall > applySettingsCall);

        final int apply = source.indexOf("private void applyActivitySettings(ActivityLayoutSnapshot snapshot)");
        final int captureStart = source.indexOf("private ActivityLayoutSnapshot captureCurrentActivity()", apply);
        final String applyBody = source.substring(apply, captureStart);
        assertTrue(applyBody.contains("if (snapshot.hasAppearance())"));
        assertTrue(applyBody.contains("\"buttonScale\", appearance.getButtonScale()"));
        assertTrue(applyBody.contains("\"gap\", appearance.getGap()"));
        assertTrue(applyBody.contains("\"idleOpacity\", appearance.getIdleOpacity()"));
        assertTrue(applyBody.contains("\"hoverOpacity\", appearance.getHoverOpacity()"));
        assertTrue(applyBody.contains("\"selectedOpacity\", appearance.getSelectedOpacity()"));
        assertTrue(applyBody.contains("\"frameOpacity\", appearance.getFrameOpacity()"));
        assertTrue(applyBody.contains("\"showTooltip\", appearance.isShowTooltip()"));
        assertTrue(applyBody.contains("\"buttonColor\", appearance.getButtonColor()"));
        assertTrue(applyBody.contains("\"hoverColor\", appearance.getHoverColor()"));
        assertTrue(applyBody.contains("\"selectedColor\", appearance.getSelectedColor()"));
        assertTrue(applyBody.contains("\"borderColor\", appearance.getBorderColor()"));
    }
}
