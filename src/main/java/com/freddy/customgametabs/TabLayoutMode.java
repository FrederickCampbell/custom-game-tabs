package com.freddy.customgametabs;

public enum TabLayoutMode
{
    DOCKED("Docked"),
    FREEFORM("Freeform");

    private final String displayName;

    TabLayoutMode(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
