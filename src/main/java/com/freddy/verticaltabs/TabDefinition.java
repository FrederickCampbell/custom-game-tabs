package com.freddy.verticaltabs;

final class TabDefinition
{
    private final String name;
    private final String fallback;

    TabDefinition(String name, String fallback)
    {
        this.name = name;
        this.fallback = fallback;
    }

    String getName()
    {
        return name;
    }

    String getFallback()
    {
        return fallback;
    }
}
