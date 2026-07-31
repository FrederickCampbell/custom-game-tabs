package com.freddy.verticaltabs;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class VerticalTabsPluginTest
{
    private VerticalTabsPluginTest()
    {
    }

    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(VerticalTabsPlugin.class);
        RuneLite.main(args);
    }
}
