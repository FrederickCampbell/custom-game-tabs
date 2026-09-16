package com.freddy.customgametabs;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class CustomGameTabsPluginTest
{
    private CustomGameTabsPluginTest()
    {
    }

    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(CustomGameTabsPlugin.class);
        RuneLite.main(args);
    }
}
