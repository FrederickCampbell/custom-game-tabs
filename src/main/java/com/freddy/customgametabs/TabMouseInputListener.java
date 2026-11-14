package com.freddy.customgametabs;

import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import net.runelite.client.input.MouseAdapter;

/**
 * Routes AWT mouse gestures to custom tabs without reading widget state.
 *
 * The Target supplies hit-testing from published snapshots and queues mutations
 * onto the client thread. This listener owns only the transient capture and
 * click-suppression lifecycle; it never activates a tab or mutates a layout
 * directly on the AWT thread.
 */
final class TabMouseInputListener extends MouseAdapter
{
    interface Target
    {
        boolean isMenuOpen();
        boolean canStartAltDrag(int x, int y);
        boolean ownsInputSurface(int x, int y);
        boolean isDrawerHandle(int x, int y);
        int tabAt(int x, int y);
        boolean isDrawerTab(int tabIndex, int x, int y);
        void updateHover(int x, int y);
        void clearHover();
        void beginAltDrag(int x, int y);
        void previewAltDrag(int x, int y);
        void finishAltDrag(int x, int y);
        void cancelAltDrag();
        void toggleDrawer();
        void activateTab(int tabIndex, boolean fromDrawer);
    }

    private final Target target;
    private final InputGestureCapture capture = new InputGestureCapture();

    TabMouseInputListener(Target target)
    {
        this.target = target;
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent event)
    {
        if (!target.isMenuOpen())
        {
            target.updateHover(event.getX(), event.getY());
        }
        return event;
    }

    @Override
    public MouseEvent mouseDragged(MouseEvent event)
    {
        if (target.isMenuOpen())
        {
            return event;
        }

        if (capture.isTabPressCaptured())
        {
            event.consume();
            return event;
        }

        if (capture.isAltDragCaptured())
        {
            target.previewAltDrag(event.getX(), event.getY());
            event.consume();
            return event;
        }

        if (!event.isAltDown())
        {
            target.updateHover(event.getX(), event.getY());
        }
        return event;
    }

    @Override
    public MouseEvent mouseExited(MouseEvent event)
    {
        if (!target.isMenuOpen())
        {
            target.clearHover();
        }
        return event;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent event)
    {
        if (target.isMenuOpen())
        {
            capture.resetClickGesture();
            capture.stopAltDrag();
            target.cancelAltDrag();
            return event;
        }

        if (event.isAltDown() && SwingUtilities.isLeftMouseButton(event))
        {
            final int x = event.getX();
            final int y = event.getY();
            if (target.canStartAltDrag(x, y))
            {
                capture.captureAltDrag();
                target.beginAltDrag(x, y);
                event.consume();
            }
            else
            {
                capture.stopAltDrag();
            }
            return event;
        }

        capture.stopAltDrag();
        target.cancelAltDrag();
        final int x = event.getX();
        final int y = event.getY();
        if (!target.ownsInputSurface(x, y) || !SwingUtilities.isLeftMouseButton(event))
        {
            return event;
        }

        capture.captureTabPress();
        event.consume();

        if (target.isDrawerHandle(x, y))
        {
            target.toggleDrawer();
            return event;
        }

        final int tabIndex = target.tabAt(x, y);
        if (tabIndex >= 0)
        {
            target.activateTab(tabIndex, target.isDrawerTab(tabIndex, x, y));
        }
        return event;
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent event)
    {
        if (capture.isAltDragCaptured())
        {
            if (!SwingUtilities.isLeftMouseButton(event))
            {
                return event;
            }
            final int endX = event.getX();
            final int endY = event.getY();
            capture.releaseAltDrag();
            target.finishAltDrag(endX, endY);
            event.consume();
            return event;
        }

        if (target.isMenuOpen())
        {
            capture.releaseTabPress();
            return event;
        }

        if (SwingUtilities.isLeftMouseButton(event) && capture.releaseTabPress())
        {
            event.consume();
        }
        return event;
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent event)
    {
        if (target.isMenuOpen())
        {
            capture.clearPendingLeftClick();
            return event;
        }

        if (SwingUtilities.isLeftMouseButton(event) && capture.consumePendingLeftClick())
        {
            event.consume();
        }
        return event;
    }

    void resetClickGesture()
    {
        capture.resetClickGesture();
    }

    void stopAltDrag()
    {
        capture.stopAltDrag();
    }

    void resetAll()
    {
        capture.resetAll();
    }
}
