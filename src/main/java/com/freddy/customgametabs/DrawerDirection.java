package com.freddy.customgametabs;

public enum DrawerDirection
{
    AUTO("Auto"),
    UP("Up"),
    DOWN("Down"),
    LEFT("Left"),
    RIGHT("Right");

    private final String label;

    DrawerDirection(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
