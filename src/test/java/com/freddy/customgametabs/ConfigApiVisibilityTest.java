package com.freddy.customgametabs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ConfigApiVisibilityTest
{
    @Test
    public void configExposedPluginEnumsRemainPublic() throws IOException
    {
        final String layoutMode = Files.readString(Path.of(
            "src", "main", "java", "com", "freddy", "customgametabs", "TabLayoutMode.java"
        ));
        final String drawerDirection = Files.readString(Path.of(
            "src", "main", "java", "com", "freddy", "customgametabs", "DrawerDirection.java"
        ));

        assertTrue(
            "TabLayoutMode is exposed by CustomGameTabsConfig and must stay public for RuneLite's config proxy",
            layoutMode.contains("public enum TabLayoutMode")
        );
        assertTrue(
            "DrawerDirection is exposed by CustomGameTabsConfig and must stay public for RuneLite's config proxy",
            drawerDirection.contains("public enum DrawerDirection")
        );
    }
}
