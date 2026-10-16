package com.ceboxxx.simplelog;

/** Tracks package transitions, not activity transitions inside a package. */
final class ForegroundTracker {
    private String foreground;
    ForegroundTracker(String foreground) { this.foreground = foreground; }
    boolean resume(String pkg) {
        if (pkg == null || pkg.equals(foreground)) return false;
        foreground = pkg;
        return true;
    }
    void reset() { foreground = null; }
    String current() { return foreground; }
}
