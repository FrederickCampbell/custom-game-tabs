package com.freddy.customgametabs;

import org.junit.Test;
import static org.junit.Assert.*;

public class DrawerAnimationContractTest
{
    @Test
    public void repeatedTickUpdatesDoNotRestartAnimation()
    {
        DrawerAnimation animation = new DrawerAnimation();
        long start = 1_000_000_000L;
        animation.setExpanded(true, start);
        for (int ms = 20; ms <= 160; ms += 20)
        {
            animation.setExpanded(true, start + ms * 1_000_000L);
        }
        assertEquals(1, animation.progress(start + DrawerAnimation.DURATION_NANOS), 0);
    }

    @Test
    public void reversingMidAnimationPreservesContinuityAndFinishes()
    {
        DrawerAnimation animation = new DrawerAnimation();
        long start = 1_000_000_000L;
        animation.setExpanded(true, start);
        long reverse = start + DrawerAnimation.DURATION_NANOS / 2;
        double before = animation.progress(reverse);
        assertTrue(before > 0 && before < 1);
        animation.setExpanded(false, reverse);
        assertEquals(before, animation.progress(reverse), 0);
        assertEquals(0, animation.progress(reverse + DrawerAnimation.DURATION_NANOS), 0);
    }
}
