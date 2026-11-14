package com.freddy.customgametabs;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TabLayoutsPanelTest
{
    @Test
    public void tabControlsFitTheStandardSidebarAndHaveAccessibleNames() throws Exception
    {
        CustomGameTabsPlugin plugin = mock(CustomGameTabsPlugin.class);
        when(plugin.getLayoutMode()).thenReturn(TabLayoutMode.FREEFORM);
        when(plugin.getButtonsPerRow()).thenReturn(2);
        when(plugin.getDrawerDirection()).thenReturn(DrawerDirection.AUTO);
        when(plugin.getTabOrder()).thenReturn(IntStream.range(0, 14).boxed().collect(Collectors.toList()));
        when(plugin.getTabState(anyInt())).thenReturn(TabState.MAIN);
        when(plugin.isDrawerEnabled()).thenReturn(true);
        when(plugin.isLayoutAvailable()).thenReturn(true);
        SwingUtilities.invokeAndWait(() ->
        {
            TabLayoutsPanel panel = new TabLayoutsPanel(plugin);
            panel.setSize(225, panel.getPreferredSize().height);
            layout(panel);
            for (int tab = 0; tab < 14; tab++)
            {
                Component state = find(panel, "state-" + tab);
                assertNotNull(state);
                assertTrue(state instanceof JComboBox);
                assertEquals(LayoutSpec.TABS[tab].getName() + " placement",
                    state.getAccessibleContext().getAccessibleName());
                assertTrue(state.getX() + state.getWidth() <= state.getParent().getWidth());
                Component down = find(panel, "down-" + tab);
                assertTrue(down.getX() + down.getWidth() <= down.getParent().getWidth());
                assertTrue(down.getHeight() >= 24);
                assertTrue("Each tab row must stay compact", down.getParent().getParent().getHeight() <= 32);
            }
            BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try { panel.paint(graphics); }
            finally { graphics.dispose(); }
            try
            {
                File directory = new File("build/reports/ui");
                directory.mkdirs();
                ImageIO.write(image, "png", new File(directory, "tab-layouts.png"));
            }
            catch (Exception e) { throw new AssertionError(e); }
        });
    }

    private static Component find(Component component, String name)
    {
        if (name.equals(component.getName())) return component;
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents())
            {
                Component found = find(child, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void layout(Container component)
    {
        component.doLayout();
        for (Component child : component.getComponents())
        {
            if (child instanceof Container) layout((Container) child);
        }
    }
}
