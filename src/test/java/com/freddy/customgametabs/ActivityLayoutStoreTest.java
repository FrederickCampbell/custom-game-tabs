package com.freddy.customgametabs;

import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ActivityLayoutStoreTest
{
    @Test
    public void unchangedTextReusesSnapshotButExternalChangeAndRemovalInvalidate()
    {
        ConfigManager manager = mock(ConfigManager.class);
        String key = "freeformWorking";
        when(manager.getConfiguration(CustomGameTabsConfig.GROUP, key)).thenReturn("1|800|600|3,100,100");
        ActivityLayoutStore store = new ActivityLayoutStore(manager);
        ActivityLayoutSnapshot initial = store.loadWorking();
        assertNotNull(initial);
        assertSame(initial, store.loadWorking());
        when(manager.getConfiguration(CustomGameTabsConfig.GROUP, key)).thenReturn("1|800|600|3,200,100");
        assertNotSame(initial, store.loadWorking());
        when(manager.getConfiguration(CustomGameTabsConfig.GROUP, key)).thenReturn(null);
        assertFalse(store.hasWorking());
    }
}
