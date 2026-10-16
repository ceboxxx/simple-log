package com.ceboxxx.simplelog;
import org.junit.Test;
import static org.junit.Assert.*;
public class ForegroundTrackerTest {
    @Test public void ignoresInternalScreensButRecordsReturning() {
        ForegroundTracker t = new ForegroundTracker(null);
        assertTrue(t.resume("spotify"));
        assertFalse(t.resume("spotify"));
        assertTrue(t.resume("whatsapp"));
        assertTrue(t.resume("spotify"));
    }
    @Test public void homeAndLockAllowAnotherOpening() {
        ForegroundTracker t = new ForegroundTracker("spotify");
        assertTrue(t.resume("launcher"));
        assertTrue(t.resume("spotify"));
        t.reset();
        assertTrue(t.resume("spotify"));
    }
    @Test public void restoresStateBetweenCollections() {
        ForegroundTracker t = new ForegroundTracker("spotify");
        assertFalse(t.resume("spotify"));
        assertEquals("spotify", t.current());
    }
}
