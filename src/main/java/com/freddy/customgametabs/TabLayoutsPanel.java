package com.freddy.customgametabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/** Live activity-layout editor. Unlike ConfigPanel, every control can refresh after preset loads. */
final class TabLayoutsPanel extends PluginPanel
{
    private static final Color MUTED_TEXT = new Color(170, 170, 170);

    private final CustomGameTabsPlugin plugin;
    private final JLabel status = new JLabel(" ");
    private boolean refreshing;

    TabLayoutsPanel(CustomGameTabsPlugin plugin)
    {
        this.plugin = plugin;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        rebuild();
    }

    void refresh()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }
        rebuild();
    }

    void showStatus(String text)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> showStatus(text));
            return;
        }
        status.setText(text == null || text.isBlank() ? " " : text);
    }

    private void rebuild()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(this::rebuild);
            return;
        }

        refreshing = true;
        final String statusText = status.getText();
        removeAll();

        addTitle("Activity Layouts", Color.WHITE);
        add(Box.createRigidArea(new Dimension(0, 7)));
        addPresets();
        addSpacerSeparator();
        addLayoutControls();
        addSpacerSeparator();
        addTabControls();
        addSpacerSeparator();
        addDrawerControls();
        addSpacerSeparator();
        addFreeformControls();
        add(Box.createRigidArea(new Dimension(0, 7)));

        status.setForeground(MUTED_TEXT);
        status.setHorizontalAlignment(SwingConstants.LEFT);
        status.setAlignmentX(LEFT_ALIGNMENT);
        status.setText(statusText == null ? " " : statusText);
        add(status);

        refreshing = false;
        revalidate();
        repaint();
    }

    private void addPresets()
    {
        addSectionTitle("Presets");
        for (int slot = 1; slot <= ActivityLayoutStore.PRESET_COUNT; slot++)
        {
            final int presetSlot = slot;
            final JPanel row = rowPanel();
            final JLabel label = new JLabel("Layout " + slot);
            label.setForeground(Color.WHITE);
            row.add(label, BorderLayout.WEST);

            final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
            actions.setBackground(ColorScheme.DARK_GRAY_COLOR);
            final JButton load = new JButton("Load");
            final JButton save = new JButton("Save");
            load.setEnabled(plugin.hasActivityPreset(slot));
            load.setToolTipText("Load the complete activity layout");
            save.setToolTipText("Save the complete current activity layout");
            load.addActionListener(e -> plugin.loadActivityPreset(presetSlot));
            save.addActionListener(e -> plugin.saveActivityPreset(presetSlot));
            actions.add(load);
            actions.add(save);
            row.add(actions, BorderLayout.EAST);
            add(row);
            add(Box.createRigidArea(new Dimension(0, 3)));
        }
    }

    private void addLayoutControls()
    {
        addSectionTitle("Layout");

        final JComboBox<TabLayoutMode> mode = new JComboBox<>(TabLayoutMode.values());
        mode.setSelectedItem(plugin.getLayoutMode());
        mode.addActionListener(e ->
        {
            if (!refreshing)
            {
                plugin.setLayoutMode((TabLayoutMode) mode.getSelectedItem());
            }
        });
        add(labeledControl("Mode", mode));

        final JSpinner rows = new JSpinner(new SpinnerNumberModel(
            plugin.getButtonsPerRow(),
            1,
            LayoutSpec.TABS.length,
            1
        ));
        rows.addChangeListener(e ->
        {
            if (!refreshing)
            {
                plugin.setButtonsPerRow((Integer) rows.getValue());
            }
        });
        add(labeledControl("Buttons per row", rows));
    }

    private void addTabControls()
    {
        addSectionTitle("Tabs");
        final List<Integer> order = plugin.getTabOrder();

        for (int position = 0; position < order.size(); position++)
        {
            final int tab = order.get(position);
            final JPanel row = rowPanel();
            final JLabel label = new JLabel(LayoutSpec.TABS[tab].getName());
            label.setForeground(Color.WHITE);
            row.add(label, BorderLayout.WEST);

            final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            actions.setBackground(ColorScheme.DARK_GRAY_COLOR);

            final JComboBox<TabState> state = new JComboBox<>(TabState.values());
            state.setSelectedItem(plugin.getTabState(tab));
            state.setPreferredSize(new Dimension(78, 22));
            state.addActionListener(e ->
            {
                if (!refreshing)
                {
                    plugin.setTabState(tab, (TabState) state.getSelectedItem());
                }
            });

            final JButton up = new JButton("↑");
            final JButton down = new JButton("↓");
            compactButton(up);
            compactButton(down);
            up.setEnabled(position > 0);
            down.setEnabled(position < order.size() - 1);
            up.setToolTipText("Move earlier");
            down.setToolTipText("Move later");
            up.addActionListener(e -> plugin.moveTab(tab, -1));
            down.addActionListener(e -> plugin.moveTab(tab, 1));

            actions.add(state);
            actions.add(up);
            actions.add(down);
            row.add(actions, BorderLayout.EAST);
            add(row);
            add(Box.createRigidArea(new Dimension(0, 2)));
        }
    }

    private void addDrawerControls()
    {
        addSectionTitle("Drawer");

        final JCheckBox enabled = new JCheckBox();
        enabled.setSelected(plugin.isDrawerEnabled());
        enabled.addActionListener(e ->
        {
            if (!refreshing)
            {
                plugin.setDrawerEnabled(enabled.isSelected());
            }
        });
        add(labeledControl("Enabled", enabled));

        final JComboBox<DrawerDirection> direction = new JComboBox<>(DrawerDirection.values());
        direction.setSelectedItem(plugin.getDrawerDirection());
        direction.setEnabled(plugin.isDrawerEnabled());
        direction.addActionListener(e ->
        {
            if (!refreshing)
            {
                plugin.setDrawerDirection((DrawerDirection) direction.getSelectedItem());
            }
        });
        add(labeledControl("Direction", direction));

        final JCheckBox open = new JCheckBox();
        open.setSelected(plugin.isDrawerExpanded());
        open.setEnabled(plugin.isDrawerEnabled());
        open.addActionListener(e ->
        {
            if (!refreshing)
            {
                plugin.setDrawerOpen(open.isSelected());
            }
        });
        add(labeledControl("Open now", open));

        final JCheckBox closeAfter = new JCheckBox();
        closeAfter.setSelected(plugin.isDrawerCloseAfterSelection());
        closeAfter.setEnabled(plugin.isDrawerEnabled());
        closeAfter.addActionListener(e ->
        {
            if (!refreshing)
            {
                plugin.setDrawerCloseAfterSelection(closeAfter.isSelected());
            }
        });
        add(labeledControl("Close after selection", closeAfter));
    }

    private void addFreeformControls()
    {
        addSectionTitle("Freeform");
        final boolean freeform = plugin.getLayoutMode() == TabLayoutMode.FREEFORM;

        final JCheckBox stick = new JCheckBox();
        stick.setSelected(plugin.isStickTogether());
        stick.setEnabled(freeform);
        stick.setToolTipText("When on, tabs that are currently snapped together move as one. Turn off to separate them again.");
        stick.addActionListener(e ->
        {
            if (!refreshing)
            {
                plugin.setStickTogether(stick.isSelected());
            }
        });
        add(labeledControl("Stick Together", stick));

        final JButton reset = wideButton("Reset Working Layout");
        reset.setEnabled(freeform);
        reset.setToolTipText("Reassemble main tabs using Buttons per row. Saved presets are not changed.");
        reset.addActionListener(e -> plugin.resetWorkingLayout());
        add(reset);
        add(Box.createRigidArea(new Dimension(0, 4)));

        final JButton restore = wideButton("Restore Previous Layout");
        restore.setEnabled(plugin.hasPreviousLayout());
        restore.setToolTipText("Restore the complete layout from immediately before the last reset or preset load.");
        restore.addActionListener(e -> plugin.restorePreviousLayout());
        add(restore);
    }

    static BufferedImage createNavigationIcon()
    {
        final BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = image.createGraphics();
        try
        {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            final Color outline = new Color(79, 54, 28);
            final Color gold = new Color(219, 166, 67);
            graphics.setColor(outline);
            graphics.fillRoundRect(1, 1, 6, 6, 2, 2);
            graphics.fillRoundRect(9, 1, 6, 6, 2, 2);
            graphics.fillRoundRect(1, 9, 6, 6, 2, 2);
            graphics.fillRoundRect(9, 9, 6, 6, 2, 2);
            graphics.setColor(gold);
            graphics.fillRoundRect(2, 2, 4, 4, 1, 1);
            graphics.fillRoundRect(10, 2, 4, 4, 1, 1);
            graphics.fillRoundRect(2, 10, 4, 4, 1, 1);
            graphics.fillRoundRect(10, 10, 4, 4, 1, 1);
        }
        finally
        {
            graphics.dispose();
        }
        return image;
    }

    private void addTitle(String text, Color color)
    {
        final JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(FontManager.getRunescapeBoldFont());
        label.setAlignmentX(LEFT_ALIGNMENT);
        add(label);
    }

    private void addSectionTitle(String text)
    {
        addTitle(text, ColorScheme.BRAND_ORANGE);
        add(Box.createRigidArea(new Dimension(0, 4)));
    }

    private void addSpacerSeparator()
    {
        add(Box.createRigidArea(new Dimension(0, 7)));
        final JSeparator separator = new JSeparator();
        separator.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
        separator.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        separator.setAlignmentX(LEFT_ALIGNMENT);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setBorder(BorderFactory.createEmptyBorder());
        add(separator);
        add(Box.createRigidArea(new Dimension(0, 7)));
    }

    private JPanel rowPanel()
    {
        final JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(ColorScheme.DARK_GRAY_COLOR);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return row;
    }

    private JPanel labeledControl(String labelText, java.awt.Component control)
    {
        final JPanel row = rowPanel();
        final JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        row.add(label, BorderLayout.WEST);
        row.add(control, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setBorder(new EmptyBorder(0, 0, 3, 0));
        return row;
    }

    private static JButton wideButton(String text)
    {
        final JButton button = new JButton(text);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
        return button;
    }

    private static void compactButton(JButton button)
    {
        button.setMargin(new java.awt.Insets(1, 4, 1, 4));
        button.setPreferredSize(new Dimension(24, 22));
    }
}
