package com.freddy.customgametabs;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptEvent;
import net.runelite.api.ScriptEventBuilder;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class NativeTabActivationTest
{
    @Test
    public void everyTabForwardsItsOwnLiveWidgetListenerAndOperation()
    {
        Client client = mock(Client.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getTopLevelInterfaceId()).thenReturn(InterfaceID.TOPLEVEL_PRE_EOC);
        NativeTabController controller = new NativeTabController(client);
        for (int tab = 0; tab < LayoutSpec.TABS.length; tab++)
        {
            Widget stone = mock(Widget.class);
            Object[] listener = {123, tab};
            when(client.getWidget(LayoutSpec.MODERN.getStoneId(tab))).thenReturn(stone);
            when(stone.getOnOpListener()).thenReturn(listener);
            ScriptEventBuilder builder = mock(ScriptEventBuilder.class, RETURNS_SELF);
            ScriptEvent event = mock(ScriptEvent.class);
            when(client.createScriptEventBuilder(listener)).thenReturn(builder);
            when(builder.build()).thenReturn(event);
            controller.activateTab(tab);
            verify(builder).setSource(stone);
            verify(builder).setOp(1);
            verify(event).run();
        }
    }

    @Test
    public void staleQueuedActivationCannotRunAfterLogoutOrInUnsupportedLayout()
    {
        Client client = mock(Client.class);
        NativeTabController controller = new NativeTabController(client);
        when(client.getTopLevelInterfaceId()).thenReturn(InterfaceID.TOPLEVEL_PRE_EOC);
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        controller.activateTab(0);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getTopLevelInterfaceId()).thenReturn(-1);
        controller.activateTab(0);
        verify(client, never()).getWidget(anyInt());
    }
}
