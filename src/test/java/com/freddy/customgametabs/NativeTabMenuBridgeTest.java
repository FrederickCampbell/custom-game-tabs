package com.freddy.customgametabs;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.client.eventbus.EventBus;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NativeTabMenuBridgeTest
{
    private static final int COMBAT_STONE = 0x123400;

    @Test
    public void nonOwnedInventorySurfaceNeverTouchesTheNativeMenu()
    {
        final Client client = mock(Client.class);
        final EventBus eventBus = mock(EventBus.class);
        final Menu nativeInventoryMenu = mock(Menu.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getMenu()).thenReturn(nativeInventoryMenu);

        new NativeTabMenuBridge(client, eventBus)
            .prepareMenu(LayoutSpec.INVENTORY_TAB, false);

        verify(client, never()).getMenu();
        verifyNoInteractions(nativeInventoryMenu, eventBus);
    }

    @Test
    public void realTabActionRemainsDefaultWithoutRemovingManagedWidgetOptions()
    {
        final Menu menu = mock(Menu.class);
        final MenuEntry cancel = mock(MenuEntry.class);
        final MenuEntry nativeSecondary = mock(MenuEntry.class);
        final MenuEntry primary = primary(COMBAT_STONE);
        final MenuEntry reorder = mock(MenuEntry.class);
        final MenuEntry reset = mock(MenuEntry.class);
        when(reorder.getType()).thenReturn(MenuAction.RUNELITE);
        when(reset.getType()).thenReturn(MenuAction.RUNELITE);
        // The higher-index managed options are what Mouse Highlight would
        // otherwise display when hovering over the Combat/Sailing stone.
        when(menu.getMenuEntries()).thenReturn(new MenuEntry[]
            {cancel, nativeSecondary, primary, reorder, reset});

        NativeTabMenuBridge.prioritizeNativePrimaryAction(menu, COMBAT_STONE);

        final ArgumentCaptor<MenuEntry[]> captured = ArgumentCaptor.forClass(MenuEntry[].class);
        verify(menu).setMenuEntries(captured.capture());
        final MenuEntry[] result = captured.getValue();
        assertArrayEquals(new MenuEntry[] {cancel, nativeSecondary, reorder, reset, primary}, result);
        assertSame(primary, result[result.length - 1]);
    }

    @Test
    public void ignoresAnotherWidgetAndDoesNotDuplicateExistingDefault()
    {
        final Menu menu = mock(Menu.class);
        final MenuEntry otherWidget = primary(COMBAT_STONE + 1);
        final MenuEntry reorder = mock(MenuEntry.class);
        when(menu.getMenuEntries()).thenReturn(new MenuEntry[] {otherWidget, reorder});
        NativeTabMenuBridge.prioritizeNativePrimaryAction(menu, COMBAT_STONE);
        verify(menu, never()).setMenuEntries(any(MenuEntry[].class));

        final Menu alreadyCorrect = mock(Menu.class);
        final MenuEntry primary = primary(COMBAT_STONE);
        when(alreadyCorrect.getMenuEntries()).thenReturn(new MenuEntry[] {reorder, primary});
        NativeTabMenuBridge.prioritizeNativePrimaryAction(alreadyCorrect, COMBAT_STONE);
        verify(alreadyCorrect, never()).setMenuEntries(any(MenuEntry[].class));
    }

    @Test
    public void retainsUnalteredEntriesWhenThereIsNoNativePrimaryOperation()
    {
        final Menu menu = mock(Menu.class);
        final MenuEntry secondary = mock(MenuEntry.class);
        when(secondary.getType()).thenReturn(MenuAction.CC_OP_LOW_PRIORITY);
        final MenuEntry pluginOption = mock(MenuEntry.class);
        when(pluginOption.getType()).thenReturn(MenuAction.RUNELITE);
        when(menu.getMenuEntries()).thenReturn(new MenuEntry[] {secondary, pluginOption});
        NativeTabMenuBridge.prioritizeNativePrimaryAction(menu, COMBAT_STONE);
        verify(menu, never()).setMenuEntries(any(MenuEntry[].class));
    }

    private static MenuEntry primary(int widgetId)
    {
        final MenuEntry entry = mock(MenuEntry.class);
        when(entry.getType()).thenReturn(MenuAction.CC_OP);
        when(entry.getIdentifier()).thenReturn(1);
        when(entry.getParam0()).thenReturn(-1);
        when(entry.getParam1()).thenReturn(widgetId);
        return entry;
    }
}
