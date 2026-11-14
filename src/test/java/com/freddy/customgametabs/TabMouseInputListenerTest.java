package com.freddy.customgametabs;

import java.awt.Canvas;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** AWT dispatch contracts independent of RuneLite's widget state. */
public class TabMouseInputListenerTest
{
    @Test
    public void leftTabPressActivatesOnceAndSuppressesReleaseAndClick()
    {
        TabMouseInputListener.Target target = mock(TabMouseInputListener.Target.class);
        when(target.ownsInputSurface(110, 120)).thenReturn(true);
        when(target.tabAt(110, 120)).thenReturn(LayoutSpec.INVENTORY_TAB);
        TabMouseInputListener listener = new TabMouseInputListener(target);

        assertTrue(listener.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 110, 120,
            0, MouseEvent.BUTTON1)).isConsumed());
        verify(target).activateTab(LayoutSpec.INVENTORY_TAB, false);
        assertTrue(listener.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 110, 120,
            0, MouseEvent.BUTTON1)).isConsumed());
        assertTrue(listener.mouseClicked(mouse(MouseEvent.MOUSE_CLICKED, 110, 120,
            0, MouseEvent.BUTTON1)).isConsumed());
        assertFalse(listener.mouseClicked(mouse(MouseEvent.MOUSE_CLICKED, 110, 120,
            0, MouseEvent.BUTTON1)).isConsumed());
        verify(target, times(1)).activateTab(anyInt(), anyBoolean());
    }

    @Test
    public void nativeInventorySurfacePassesThroughWithoutActivation()
    {
        TabMouseInputListener.Target target = mock(TabMouseInputListener.Target.class);
        TabMouseInputListener listener = new TabMouseInputListener(target);

        assertFalse(listener.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 100, 120,
            0, MouseEvent.BUTTON1)).isConsumed());
        assertFalse(listener.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 100, 120,
            0, MouseEvent.BUTTON1)).isConsumed());
        assertFalse(listener.mouseClicked(mouse(MouseEvent.MOUSE_CLICKED, 100, 120,
            0, MouseEvent.BUTTON1)).isConsumed());
        verify(target, never()).tabAt(anyInt(), anyInt());
        verify(target, never()).activateTab(anyInt(), anyBoolean());
    }

    @Test
    public void altDragQueuesBeginPreviewFinishAndSwallowsClick()
    {
        TabMouseInputListener.Target target = mock(TabMouseInputListener.Target.class);
        when(target.canStartAltDrag(110, 120)).thenReturn(true);
        TabMouseInputListener listener = new TabMouseInputListener(target);
        int mods = InputEvent.ALT_DOWN_MASK | InputEvent.BUTTON1_DOWN_MASK;

        assertTrue(listener.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 110, 120,
            mods, MouseEvent.BUTTON1)).isConsumed());
        assertTrue(listener.mouseDragged(mouse(MouseEvent.MOUSE_DRAGGED, 130, 145,
            mods, MouseEvent.NOBUTTON)).isConsumed());
        assertTrue(listener.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 130, 145,
            InputEvent.ALT_DOWN_MASK, MouseEvent.BUTTON1)).isConsumed());
        assertTrue(listener.mouseClicked(mouse(MouseEvent.MOUSE_CLICKED, 130, 145,
            InputEvent.ALT_DOWN_MASK, MouseEvent.BUTTON1)).isConsumed());
        verify(target).beginAltDrag(110, 120);
        verify(target).previewAltDrag(130, 145);
        verify(target).finishAltDrag(130, 145);
        verify(target, never()).activateTab(anyInt(), anyBoolean());
    }

    @Test
    public void menuOpeningAbandonsCapturedTabWithoutSwallowingNativeClicks()
    {
        TabMouseInputListener.Target target = mock(TabMouseInputListener.Target.class);
        when(target.ownsInputSurface(110, 120)).thenReturn(true);
        TabMouseInputListener listener = new TabMouseInputListener(target);

        assertTrue(listener.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 110, 120,
            0, MouseEvent.BUTTON1)).isConsumed());
        when(target.isMenuOpen()).thenReturn(true);
        assertFalse(listener.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 110, 120,
            0, MouseEvent.BUTTON1)).isConsumed());
        assertFalse(listener.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 110, 120,
            0, MouseEvent.BUTTON1)).isConsumed());
        assertFalse(listener.mouseClicked(mouse(MouseEvent.MOUSE_CLICKED, 110, 120,
            0, MouseEvent.BUTTON1)).isConsumed());
        // The first ordinary press and the later menu-open press each cancel
        // any pending client-thread Alt-drag state, matching the old adapter.
        verify(target, times(2)).cancelAltDrag();
    }

    @Test
    public void rightReleaseDoesNotConsumePendingLeftClick()
    {
        TabMouseInputListener.Target target = mock(TabMouseInputListener.Target.class);
        when(target.ownsInputSurface(110, 120)).thenReturn(true);
        TabMouseInputListener listener = new TabMouseInputListener(target);
        listener.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 110, 120,
            0, MouseEvent.BUTTON1));
        assertFalse(listener.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 110, 120,
            0, MouseEvent.BUTTON3)).isConsumed());
        assertFalse(listener.mouseClicked(mouse(MouseEvent.MOUSE_CLICKED, 110, 120,
            0, MouseEvent.BUTTON3)).isConsumed());
        assertTrue(listener.mouseClicked(mouse(MouseEvent.MOUSE_CLICKED, 110, 120,
            0, MouseEvent.BUTTON1)).isConsumed());
    }

    private static MouseEvent mouse(int id, int x, int y, int modifiers, int button)
    {
        return new MouseEvent(new Canvas(), id, 0, modifiers, x, y, 1, false, button);
    }
}
