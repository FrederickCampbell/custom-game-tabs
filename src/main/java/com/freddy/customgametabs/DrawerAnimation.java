package com.freddy.customgametabs;

/** A reversible transition; repeatedly requesting the same target is a no-op. */
final class DrawerAnimation
{
    static final long DURATION_NANOS = 170_000_000L;
    private boolean expanded;
    private double from;
    private long started;

    void setExpanded(boolean expanded, long now)
    {
        if (this.expanded == expanded)
        {
            return;
        }
        from = progress(now);
        this.expanded = expanded;
        started = now;
    }

    double progress(long now)
    {
        final double t = Math.max(0, Math.min(1, (now - started) / (double) DURATION_NANOS));
        final double eased = 1 - Math.pow(1 - t, 3);
        return from + ((expanded ? 1 : 0) - from) * eased;
    }
}
