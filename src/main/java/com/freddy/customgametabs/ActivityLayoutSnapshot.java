package com.freddy.customgametabs;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete activity-layout snapshot.
 *
 * Version 3 stores the complete activity layout plus its visual appearance.
 * Version 2 stores layout/drawer/freeform/sticky state without appearance.
 * Version 1 is the old 2.1.x freeform-position-only format. Both remain
 * readable so existing presets migrate without being destroyed.
 */
final class ActivityLayoutSnapshot
{
    private static final String VERSION = "3";
    private static final int TAB_COUNT = 14;

    private final int canvasWidth;
    private final int canvasHeight;
    private final TabLayoutMode layoutMode;
    private final int buttonsPerRow;
    private final List<Integer> order;
    private final TabState[] states;
    private final boolean drawerEnabled;
    private final DrawerDirection drawerDirection;
    private final boolean drawerCloseAfterSelection;
    private final boolean drawerExpanded;
    private final Point drawerHandle;
    private final boolean stickTogether;
    private final StickyGroups stickyGroups;
    private final Map<Integer, Point> locations;
    private final Appearance appearance;
    private final boolean legacyPlacementOnly;

    ActivityLayoutSnapshot(
        int canvasWidth,
        int canvasHeight,
        TabLayoutMode layoutMode,
        int buttonsPerRow,
        List<Integer> order,
        TabState[] states,
        boolean drawerEnabled,
        DrawerDirection drawerDirection,
        boolean drawerCloseAfterSelection,
        boolean drawerExpanded,
        Point drawerHandle,
        boolean stickTogether,
        StickyGroups stickyGroups,
        Map<Integer, Point> locations
    )
    {
        this(
            canvasWidth,
            canvasHeight,
            layoutMode,
            buttonsPerRow,
            order,
            states,
            drawerEnabled,
            drawerDirection,
            drawerCloseAfterSelection,
            drawerExpanded,
            drawerHandle,
            stickTogether,
            stickyGroups,
            locations,
            null,
            false
        );
    }

    ActivityLayoutSnapshot(
        int canvasWidth,
        int canvasHeight,
        TabLayoutMode layoutMode,
        int buttonsPerRow,
        List<Integer> order,
        TabState[] states,
        boolean drawerEnabled,
        DrawerDirection drawerDirection,
        boolean drawerCloseAfterSelection,
        boolean drawerExpanded,
        Point drawerHandle,
        boolean stickTogether,
        StickyGroups stickyGroups,
        Map<Integer, Point> locations,
        Appearance appearance
    )
    {
        this(
            canvasWidth,
            canvasHeight,
            layoutMode,
            buttonsPerRow,
            order,
            states,
            drawerEnabled,
            drawerDirection,
            drawerCloseAfterSelection,
            drawerExpanded,
            drawerHandle,
            stickTogether,
            stickyGroups,
            locations,
            appearance,
            false
        );
    }

    private ActivityLayoutSnapshot(
        int canvasWidth,
        int canvasHeight,
        TabLayoutMode layoutMode,
        int buttonsPerRow,
        List<Integer> order,
        TabState[] states,
        boolean drawerEnabled,
        DrawerDirection drawerDirection,
        boolean drawerCloseAfterSelection,
        boolean drawerExpanded,
        Point drawerHandle,
        boolean stickTogether,
        StickyGroups stickyGroups,
        Map<Integer, Point> locations,
        Appearance appearance,
        boolean legacyPlacementOnly
    )
    {
        this.canvasWidth = Math.max(1, canvasWidth);
        this.canvasHeight = Math.max(1, canvasHeight);
        this.layoutMode = layoutMode;
        this.buttonsPerRow = Math.max(1, Math.min(TAB_COUNT, buttonsPerRow));
        this.order = sanitizeOrder(order);
        this.states = sanitizeStates(states);
        this.drawerEnabled = drawerEnabled;
        this.drawerDirection = drawerDirection == null
            ? DrawerDirection.AUTO
            : drawerDirection;
        this.drawerCloseAfterSelection = drawerCloseAfterSelection;
        this.drawerExpanded = drawerExpanded;
        this.drawerHandle = drawerHandle == null ? null : new Point(drawerHandle);
        this.stickTogether = stickTogether;
        this.stickyGroups = stickyGroups == null ? new StickyGroups() : stickyGroups;
        this.locations = copyLocations(locations);
        this.appearance = appearance == null ? null : new Appearance(appearance);
        this.legacyPlacementOnly = legacyPlacementOnly;
    }

    static ActivityLayoutSnapshot parse(String encoded)
    {
        if (encoded == null || encoded.isBlank())
        {
            return null;
        }

        if (encoded.startsWith("1|"))
        {
            return parseLegacy(encoded);
        }

        if (encoded.startsWith("2|"))
        {
            return parseComplete(encoded, false);
        }

        if (encoded.startsWith(VERSION + "|"))
        {
            return parseComplete(encoded, true);
        }

        return null;
    }

    private static ActivityLayoutSnapshot parseComplete(String encoded, boolean withAppearance)
    {
        try
        {
            final String[] p = encoded.split("\\|", -1);
            final int expectedParts = withAppearance ? 16 : 15;
            if (p.length != expectedParts)
            {
                return null;
            }

            final int width = Integer.parseInt(p[1]);
            final int height = Integer.parseInt(p[2]);
            final TabLayoutMode mode = TabLayoutMode.valueOf(p[3]);
            final int buttonsAcross = Integer.parseInt(p[4]);
            final List<Integer> order = parseOrder(p[5]);
            final TabState[] states = parseStates(p[6]);
            final boolean drawerEnabled = Boolean.parseBoolean(p[7]);
            final DrawerDirection direction = DrawerDirection.valueOf(p[8]);
            final boolean close = Boolean.parseBoolean(p[9]);
            final boolean expanded = Boolean.parseBoolean(p[10]);
            final Point handle = parsePoint(p[11]);
            final boolean stick = Boolean.parseBoolean(p[12]);
            final StickyGroups groups = StickyGroups.parse(p[13]);
            final Map<Integer, Point> locations = parseLocations(p[14]);
            final Appearance appearance = withAppearance ? Appearance.parse(p[15]) : null;
            if (withAppearance && appearance == null)
            {
                return null;
            }

            return new ActivityLayoutSnapshot(
                width,
                height,
                mode,
                buttonsAcross,
                order,
                states,
                drawerEnabled,
                direction,
                close,
                expanded,
                handle,
                stick,
                groups,
                locations,
                appearance
            );
        }
        catch (IllegalArgumentException ignored)
        {
            return null;
        }
    }

    private static ActivityLayoutSnapshot parseLegacy(String encoded)
    {
        try
        {
            final String[] sections = encoded.split("\\|", 4);
            if (sections.length != 4 || !"1".equals(sections[0]))
            {
                return null;
            }

            final int width = Integer.parseInt(sections[1]);
            final int height = Integer.parseInt(sections[2]);
            final Map<Integer, Point> locations = parseLocations(sections[3]);
            if (locations.isEmpty())
            {
                return null;
            }

            return new ActivityLayoutSnapshot(
                width,
                height,
                TabLayoutMode.FREEFORM,
                1,
                defaultOrder(),
                defaultStates(),
                true,
                DrawerDirection.AUTO,
                true,
                false,
                null,
                false,
                new StickyGroups(),
                locations,
                null,
                true
            );
        }
        catch (IllegalArgumentException ignored)
        {
            return null;
        }
    }

    String encode()
    {
        final StringBuilder out = new StringBuilder();
        out.append(appearance == null ? "2" : VERSION).append('|')
            .append(canvasWidth).append('|')
            .append(canvasHeight).append('|')
            .append(layoutMode == null ? TabLayoutMode.DOCKED.name() : layoutMode.name()).append('|')
            .append(buttonsPerRow).append('|')
            .append(encodeOrder(order)).append('|')
            .append(encodeStates(states)).append('|')
            .append(drawerEnabled).append('|')
            .append(drawerDirection.name()).append('|')
            .append(drawerCloseAfterSelection).append('|')
            .append(drawerExpanded).append('|')
            .append(encodePoint(drawerHandle)).append('|')
            .append(stickTogether).append('|')
            .append(stickyGroups.encode()).append('|')
            .append(encodeLocations(locations));
        if (appearance != null)
        {
            out.append('|').append(appearance.encode());
        }
        return out.toString();
    }

    boolean isLegacyPlacementOnly()
    {
        return legacyPlacementOnly;
    }

    TabLayoutMode getLayoutMode()
    {
        return layoutMode;
    }

    int getButtonsPerRow()
    {
        return buttonsPerRow;
    }

    List<Integer> getOrder()
    {
        return new ArrayList<>(order);
    }

    TabState getState(int tabIndex)
    {
        return LayoutSpec.isValidTab(tabIndex)
            ? states[tabIndex]
            : TabState.HIDDEN;
    }

    boolean isDrawerEnabled()
    {
        return drawerEnabled;
    }

    DrawerDirection getDrawerDirection()
    {
        return drawerDirection;
    }

    boolean isDrawerCloseAfterSelection()
    {
        return drawerCloseAfterSelection;
    }

    boolean isDrawerExpanded()
    {
        return drawerExpanded;
    }

    boolean isStickTogether()
    {
        return stickTogether;
    }

    Point getDrawerHandle()
    {
        return drawerHandle == null ? null : new Point(drawerHandle);
    }

    StickyGroups getStickyGroups()
    {
        return StickyGroups.parse(stickyGroups.encode());
    }

    int getCanvasWidth()
    {
        return canvasWidth;
    }

    int getCanvasHeight()
    {
        return canvasHeight;
    }

    boolean hasAppearance()
    {
        return appearance != null;
    }

    Appearance getAppearance()
    {
        return appearance == null ? null : new Appearance(appearance);
    }

    Map<Integer, Point> resolveLocations(
        int currentCanvasWidth,
        int currentCanvasHeight,
        int buttonSize
    )
    {
        final int width = Math.max(1, currentCanvasWidth);
        final int height = Math.max(1, currentCanvasHeight);
        final int size = Math.max(1, buttonSize);
        final Map<Integer, Point> resolved = new LinkedHashMap<>();

        if (locations.isEmpty())
        {
            return resolved;
        }

        final List<List<Integer>> components = resizeComponents();
        for (List<Integer> component : components)
        {
            translateComponentForResize(
                component,
                width,
                height,
                size,
                resolved
            );
        }

        return resolved;
    }

    Point resolveDrawerHandle(
        int currentCanvasWidth,
        int currentCanvasHeight,
        int handleSize
    )
    {
        if (drawerHandle == null)
        {
            return null;
        }

        final int width = Math.max(1, currentCanvasWidth);
        final int height = Math.max(1, currentCanvasHeight);
        final int size = Math.max(1, handleSize);

        return reanchorPoint(
            drawerHandle,
            canvasWidth,
            canvasHeight,
            width,
            height,
            size
        );
    }

    /**
     * Resize each movable Freeform component as a rigid object. A sticky group
     * is one component; every non-grouped tab is its own component. Components
     * on the left half preserve their left-edge gap, components on the right
     * half preserve their right-edge gap. The same rule applies vertically.
     *
     * This deliberately does not scale coordinates or clamp each tab
     * independently. Independent clamping is what makes a right-edge cluster
     * collapse into a stack when the RuneLite canvas becomes narrower.
     */
    private List<List<Integer>> resizeComponents()
    {
        final List<List<Integer>> components = new ArrayList<>();
        final boolean[] used = new boolean[TAB_COUNT];

        if (stickTogether)
        {
            for (List<Integer> rawGroup : stickyGroups.asLists())
            {
                final List<Integer> group = new ArrayList<>();
                for (int tab : rawGroup)
                {
                    if (LayoutSpec.isValidTab(tab)
                        && locations.containsKey(tab)
                        && !used[tab])
                    {
                        group.add(tab);
                        used[tab] = true;
                    }
                }
                if (!group.isEmpty())
                {
                    components.add(group);
                }
            }
        }

        for (int tab : locations.keySet())
        {
            if (LayoutSpec.isValidTab(tab) && !used[tab])
            {
                final List<Integer> singleton = new ArrayList<>();
                singleton.add(tab);
                components.add(singleton);
                used[tab] = true;
            }
        }

        return components;
    }

    private void translateComponentForResize(
        List<Integer> component,
        int newCanvasWidth,
        int newCanvasHeight,
        int elementSize,
        Map<Integer, Point> out
    )
    {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int tab : component)
        {
            final Point point = locations.get(tab);
            if (point == null)
            {
                continue;
            }
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x + elementSize);
            maxY = Math.max(maxY, point.y + elementSize);
        }

        if (minX == Integer.MAX_VALUE)
        {
            return;
        }

        final int componentWidth = Math.max(elementSize, maxX - minX);
        final int componentHeight = Math.max(elementSize, maxY - minY);
        final double centerX = minX + componentWidth / 2.0;
        final double centerY = minY + componentHeight / 2.0;

        final boolean anchorRight = centerX >= canvasWidth / 2.0;
        final boolean anchorBottom = centerY >= canvasHeight / 2.0;

        final int desiredMinX = anchorRight
            ? newCanvasWidth - (canvasWidth - maxX) - componentWidth
            : minX;
        final int desiredMinY = anchorBottom
            ? newCanvasHeight - (canvasHeight - maxY) - componentHeight
            : minY;

        final int anchoredMinX = fitRigidAxis(
            desiredMinX,
            componentWidth,
            newCanvasWidth
        );
        final int anchoredMinY = fitRigidAxis(
            desiredMinY,
            componentHeight,
            newCanvasHeight
        );

        final int dx = anchoredMinX - minX;
        final int dy = anchoredMinY - minY;
        for (int tab : component)
        {
            final Point point = locations.get(tab);
            if (point != null)
            {
                out.put(tab, new Point(point.x + dx, point.y + dy));
            }
        }
    }

    private static Point reanchorPoint(
        Point point,
        int oldCanvasWidth,
        int oldCanvasHeight,
        int newCanvasWidth,
        int newCanvasHeight,
        int elementSize
    )
    {
        final boolean anchorRight = point.x + elementSize / 2.0 >= oldCanvasWidth / 2.0;
        final boolean anchorBottom = point.y + elementSize / 2.0 >= oldCanvasHeight / 2.0;

        final int desiredX = anchorRight
            ? newCanvasWidth - (oldCanvasWidth - (point.x + elementSize)) - elementSize
            : point.x;
        final int desiredY = anchorBottom
            ? newCanvasHeight - (oldCanvasHeight - (point.y + elementSize)) - elementSize
            : point.y;

        return new Point(
            fitRigidAxis(desiredX, elementSize, newCanvasWidth),
            fitRigidAxis(desiredY, elementSize, newCanvasHeight)
        );
    }

    private static int fitRigidAxis(int desiredMin, int span, int canvasSpan)
    {
        if (span > canvasSpan)
        {
            /* The component cannot entirely fit. Keep its internal geometry
             * intact instead of collapsing its children onto one edge. */
            return 0;
        }
        return clamp(desiredMin, 0, canvasSpan - span);
    }

    private static List<Integer> sanitizeOrder(List<Integer> input)
    {
        final List<Integer> result = new ArrayList<>();
        final boolean[] used = new boolean[TAB_COUNT];

        if (input != null)
        {
            for (Integer tab : input)
            {
                if (tab != null && LayoutSpec.isValidTab(tab) && !used[tab])
                {
                    result.add(tab);
                    used[tab] = true;
                }
            }
        }

        for (int tab = 0; tab < TAB_COUNT; tab++)
        {
            if (!used[tab])
            {
                result.add(tab);
            }
        }

        return result;
    }

    private static TabState[] sanitizeStates(TabState[] input)
    {
        final TabState[] result = new TabState[TAB_COUNT];
        for (int tab = 0; tab < TAB_COUNT; tab++)
        {
            result[tab] = input != null && tab < input.length && input[tab] != null
                ? input[tab]
                : TabState.MAIN;
        }
        return result;
    }

    private static Map<Integer, Point> copyLocations(Map<Integer, Point> input)
    {
        final Map<Integer, Point> result = new LinkedHashMap<>();
        if (input != null)
        {
            for (Map.Entry<Integer, Point> entry : input.entrySet())
            {
                if (LayoutSpec.isValidTab(entry.getKey()) && entry.getValue() != null)
                {
                    result.put(entry.getKey(), new Point(entry.getValue()));
                }
            }
        }
        return result;
    }

    private static List<Integer> parseOrder(String text)
    {
        final List<Integer> result = new ArrayList<>();
        if (text != null && !text.isBlank())
        {
            for (String token : text.split(","))
            {
                final int tab = Integer.parseInt(token.trim());
                if (LayoutSpec.isValidTab(tab) && !result.contains(tab))
                {
                    result.add(tab);
                }
            }
        }
        return sanitizeOrder(result);
    }

    private static String encodeOrder(List<Integer> order)
    {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < order.size(); i++)
        {
            if (i > 0)
            {
                out.append(',');
            }
            out.append(order.get(i));
        }
        return out.toString();
    }

    private static TabState[] parseStates(String text)
    {
        final TabState[] states = defaultStates();
        if (text == null)
        {
            return states;
        }

        for (int tab = 0; tab < Math.min(TAB_COUNT, text.length()); tab++)
        {
            switch (text.charAt(tab))
            {
                case 'D':
                    states[tab] = TabState.DRAWER;
                    break;
                case 'H':
                    states[tab] = TabState.HIDDEN;
                    break;
                case 'M':
                default:
                    states[tab] = TabState.MAIN;
                    break;
            }
        }
        return states;
    }

    private static String encodeStates(TabState[] states)
    {
        final StringBuilder out = new StringBuilder(TAB_COUNT);
        for (int tab = 0; tab < TAB_COUNT; tab++)
        {
            switch (states[tab])
            {
                case DRAWER:
                    out.append('D');
                    break;
                case HIDDEN:
                    out.append('H');
                    break;
                case MAIN:
                default:
                    out.append('M');
                    break;
            }
        }
        return out.toString();
    }

    private static Point parsePoint(String text)
    {
        if (text == null || text.isBlank())
        {
            return null;
        }

        final String[] parts = text.split(",", 2);
        if (parts.length != 2)
        {
            return null;
        }
        return new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    private static String encodePoint(Point point)
    {
        return point == null ? "" : point.x + "," + point.y;
    }

    private static Map<Integer, Point> parseLocations(String text)
    {
        final Map<Integer, Point> locations = new LinkedHashMap<>();
        if (text == null || text.isBlank())
        {
            return locations;
        }

        for (String entry : text.split(";"))
        {
            final String[] parts = entry.split(",", 3);
            if (parts.length != 3)
            {
                continue;
            }

            final int tab = Integer.parseInt(parts[0]);
            if (LayoutSpec.isValidTab(tab))
            {
                locations.put(
                    tab,
                    new Point(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]))
                );
            }
        }
        return locations;
    }

    private static String encodeLocations(Map<Integer, Point> locations)
    {
        final StringBuilder out = new StringBuilder();
        boolean first = true;
        for (Map.Entry<Integer, Point> entry : locations.entrySet())
        {
            if (!first)
            {
                out.append(';');
            }
            out.append(entry.getKey())
                .append(',')
                .append(entry.getValue().x)
                .append(',')
                .append(entry.getValue().y);
            first = false;
        }
        return out.toString();
    }

    private static List<Integer> defaultOrder()
    {
        final List<Integer> order = new ArrayList<>();
        for (int tab = 0; tab < TAB_COUNT; tab++)
        {
            order.add(tab);
        }
        return order;
    }

    private static TabState[] defaultStates()
    {
        final TabState[] states = new TabState[TAB_COUNT];
        for (int tab = 0; tab < TAB_COUNT; tab++)
        {
            states[tab] = TabState.MAIN;
        }
        return states;
    }

    static final class Appearance
    {
        private final int buttonScale;
        private final int gap;
        private final int idleOpacity;
        private final int hoverOpacity;
        private final int selectedOpacity;
        private final int frameOpacity;
        private final boolean showTooltip;
        private final Color buttonColor;
        private final Color hoverColor;
        private final Color selectedColor;
        private final Color borderColor;

        Appearance(
            int buttonScale,
            int gap,
            int idleOpacity,
            int hoverOpacity,
            int selectedOpacity,
            int frameOpacity,
            boolean showTooltip,
            Color buttonColor,
            Color hoverColor,
            Color selectedColor,
            Color borderColor
        )
        {
            this.buttonScale = clamp(buttonScale, 25, 200);
            this.gap = clamp(gap, 0, 20);
            this.idleOpacity = clamp(idleOpacity, 0, 100);
            this.hoverOpacity = clamp(hoverOpacity, 0, 100);
            this.selectedOpacity = clamp(selectedOpacity, 0, 100);
            this.frameOpacity = clamp(frameOpacity, 0, 100);
            this.showTooltip = showTooltip;
            this.buttonColor = copyColor(buttonColor, new Color(49, 38, 26, 238));
            this.hoverColor = copyColor(hoverColor, new Color(82, 62, 36, 246));
            this.selectedColor = copyColor(selectedColor, new Color(112, 82, 35, 250));
            this.borderColor = copyColor(borderColor, new Color(151, 113, 49, 248));
        }

        private Appearance(Appearance other)
        {
            this(
                other.buttonScale,
                other.gap,
                other.idleOpacity,
                other.hoverOpacity,
                other.selectedOpacity,
                other.frameOpacity,
                other.showTooltip,
                other.buttonColor,
                other.hoverColor,
                other.selectedColor,
                other.borderColor
            );
        }

        static Appearance parse(String encoded)
        {
            if (encoded == null || encoded.isBlank())
            {
                return null;
            }

            try
            {
                final String[] p = encoded.split(",", -1);
                if (p.length != 11)
                {
                    return null;
                }
                return new Appearance(
                    Integer.parseInt(p[0]),
                    Integer.parseInt(p[1]),
                    Integer.parseInt(p[2]),
                    Integer.parseInt(p[3]),
                    Integer.parseInt(p[4]),
                    Integer.parseInt(p[5]),
                    Boolean.parseBoolean(p[6]),
                    colorFromRgb(p[7]),
                    colorFromRgb(p[8]),
                    colorFromRgb(p[9]),
                    colorFromRgb(p[10])
                );
            }
            catch (IllegalArgumentException ignored)
            {
                return null;
            }
        }

        String encode()
        {
            return buttonScale + ","
                + gap + ","
                + idleOpacity + ","
                + hoverOpacity + ","
                + selectedOpacity + ","
                + frameOpacity + ","
                + showTooltip + ","
                + buttonColor.getRGB() + ","
                + hoverColor.getRGB() + ","
                + selectedColor.getRGB() + ","
                + borderColor.getRGB();
        }

        int getButtonScale() { return buttonScale; }
        int getGap() { return gap; }
        int getIdleOpacity() { return idleOpacity; }
        int getHoverOpacity() { return hoverOpacity; }
        int getSelectedOpacity() { return selectedOpacity; }
        int getFrameOpacity() { return frameOpacity; }
        boolean isShowTooltip() { return showTooltip; }
        Color getButtonColor() { return new Color(buttonColor.getRGB(), true); }
        Color getHoverColor() { return new Color(hoverColor.getRGB(), true); }
        Color getSelectedColor() { return new Color(selectedColor.getRGB(), true); }
        Color getBorderColor() { return new Color(borderColor.getRGB(), true); }

        private static Color colorFromRgb(String encoded)
        {
            return new Color(Integer.parseInt(encoded), true);
        }

        private static Color copyColor(Color value, Color fallback)
        {
            final Color source = value == null ? fallback : value;
            return new Color(source.getRGB(), true);
        }
    }

    private static int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(max, value));
    }
}
