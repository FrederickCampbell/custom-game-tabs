package com.freddy.customgametabs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DrawerAnimationContractTest
{
    @Test
    public void drawerUsesStableHandleGeometryAndAnimatedExpansion() throws IOException
    {
        final String source = Files.readString(
            Path.of(
                "src", "main", "java", "com", "freddy", "customgametabs",
                "DrawerOverlay.java"
            )
        );

        assertTrue(source.contains("ANIMATION_NANOS = 170_000_000L"));
        assertTrue(source.contains("final double animation = animationProgress();"));
        assertTrue(source.contains("setMovable(false);"));
        assertTrue(source.contains("localHandleOffset(\n            effectiveDirection,\n            true,"));
    }
}
