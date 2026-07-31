package com.freddy.verticaltabs;

final class TabDefinition
{
    private final int index;
    private final String name;
    private final String fallback;

    TabDefinition(int index, String name, String fallback)
    {
        this.index = index;
        this.name = name;
        this.fallback = fallback;
    }

    int getIndex()
    {
        return index;
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
