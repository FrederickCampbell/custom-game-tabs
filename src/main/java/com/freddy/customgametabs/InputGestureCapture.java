package com.freddy.customgametabs;

/**
 * Tracks the AWT mouse-gesture bookkeeping for the custom tab surfaces.
 *
 * Keep tab press capture and Alt-drag capture independent. In particular, an
 * Alt press while a tab press is still captured must not silently replace the
 * earlier state; the input listener historically prioritizes a captured tab
 * drag before an Alt-drag. Pending click suppression also survives a mouse
 * release until the matching clicked event.
 *
 * This class never reads client widgets or queues client-thread work.
 */
final class InputGestureCapture
{
    private boolean tabPressCaptured;
    private boolean altDragCaptured;
    private boolean pendingLeftClickSuppression;

    boolean isTabPressCaptured()
    {
        return tabPressCaptured;
    }

    boolean isAltDragCaptured()
    {
        return altDragCaptured;
    }

    void captureTabPress()
    {
        tabPressCaptured = true;
        pendingLeftClickSuppression = true;
    }

    void captureAltDrag()
    {
        altDragCaptured = true;
        pendingLeftClickSuppression = true;
    }

    void stopAltDrag()
    {
        altDragCaptured = false;
    }

    boolean releaseTabPress()
    {
        if (!tabPressCaptured)
        {
            return false;
        }
        tabPressCaptured = false;
        return true;
    }

    boolean releaseAltDrag()
    {
        if (!altDragCaptured)
        {
            return false;
        }
        altDragCaptured = false;
        return true;
    }

    boolean consumePendingLeftClick()
    {
        if (!pendingLeftClickSuppression)
        {
            return false;
        }
        pendingLeftClickSuppression = false;
        return true;
    }

    void clearPendingLeftClick()
    {
        pendingLeftClickSuppression = false;
    }

    void resetClickGesture()
    {
        tabPressCaptured = false;
        pendingLeftClickSuppression = false;
    }

    void resetAll()
    {
        resetClickGesture();
        stopAltDrag();
    }
}
