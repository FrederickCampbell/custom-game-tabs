package com.freddy.customgametabs;

import org.junit.Test;
import static org.junit.Assert.*;

/** Preserve the pre-refactor AWT gesture and click suppression contract. */
public class InputGestureCaptureTest
{
    @Test
    public void tabPressRetainsClickSuppressionAcrossRelease()
    {
        InputGestureCapture capture = new InputGestureCapture();
        capture.captureTabPress();

        assertTrue(capture.isTabPressCaptured());
        assertFalse(capture.isAltDragCaptured());
        assertTrue(capture.releaseTabPress());
        assertFalse(capture.releaseTabPress());
        assertFalse(capture.isTabPressCaptured());

        // AWT emits CLICKED after RELEASED, and only the first is swallowed.
        assertTrue(capture.consumePendingLeftClick());
        assertFalse(capture.consumePendingLeftClick());
    }

    @Test
    public void altDragReleasePreservesPendingClickSuppression()
    {
        InputGestureCapture capture = new InputGestureCapture();
        capture.captureAltDrag();

        assertTrue(capture.isAltDragCaptured());
        assertTrue(capture.releaseAltDrag());
        assertFalse(capture.isAltDragCaptured());
        assertFalse(capture.releaseAltDrag());
        assertTrue(capture.consumePendingLeftClick());
    }

    @Test
    public void cancelledAltDragDoesNotDiscardEarlierPendingClick()
    {
        InputGestureCapture capture = new InputGestureCapture();
        capture.captureAltDrag();
        capture.stopAltDrag();

        assertFalse(capture.isAltDragCaptured());
        assertTrue(capture.consumePendingLeftClick());
    }

    @Test
    public void nativeMenuOpeningClearsOnlyClickStateUntilDragIsStopped()
    {
        InputGestureCapture capture = new InputGestureCapture();
        capture.captureTabPress();
        capture.captureAltDrag();
        capture.resetClickGesture();

        assertFalse(capture.isTabPressCaptured());
        assertFalse(capture.consumePendingLeftClick());
        assertTrue(capture.isAltDragCaptured());
        capture.stopAltDrag();
        assertFalse(capture.isAltDragCaptured());
    }

    @Test
    public void simultaneousCapturesRemainIndependent()
    {
        InputGestureCapture capture = new InputGestureCapture();
        capture.captureTabPress();
        capture.captureAltDrag();

        assertTrue(capture.isTabPressCaptured());
        assertTrue(capture.isAltDragCaptured());
        assertTrue(capture.releaseTabPress());
        assertTrue(capture.isAltDragCaptured());
        assertTrue(capture.releaseAltDrag());
        assertTrue(capture.consumePendingLeftClick());
        assertFalse(capture.consumePendingLeftClick());
    }

    @Test
    public void resetAllClearsCapturesAndPendingClickedEvent()
    {
        InputGestureCapture capture = new InputGestureCapture();
        capture.captureTabPress();
        capture.captureAltDrag();
        capture.resetAll();

        assertFalse(capture.isTabPressCaptured());
        assertFalse(capture.isAltDragCaptured());
        assertFalse(capture.consumePendingLeftClick());
    }
}
