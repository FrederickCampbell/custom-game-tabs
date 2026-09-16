package com.freddy.customgametabs;

enum TabState
{
    MAIN("Main"),
    DRAWER("Drawer"),
    HIDDEN("Hidden");

    private final String label;

    TabState(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
