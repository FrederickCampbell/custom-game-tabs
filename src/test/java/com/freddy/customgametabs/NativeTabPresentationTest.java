package com.freddy.customgametabs;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class NativeTabPresentationTest
{
    @Test
    public void restoreDoesNotApplyAnOldWidgetsStateToItsReplacement()
    {
        Client client = mock(Client.class);
        when(client.getTopLevelInterfaceId()).thenReturn(InterfaceID.TOPLEVEL_PRE_EOC);
        Widget original = mock(Widget.class);
        Widget replacement = mock(Widget.class);
        when(client.getWidget(LayoutSpec.MODERN.getStoneId(0))).thenReturn(original);
        NativeTabPresentation presentation = new NativeTabPresentation(client);
        presentation.update();
        verify(original).setHidden(true);
        when(client.getWidget(LayoutSpec.MODERN.getStoneId(0))).thenReturn(replacement);
        presentation.restoreAll();
        verify(replacement, never()).setHidden(anyBoolean());
    }
}
