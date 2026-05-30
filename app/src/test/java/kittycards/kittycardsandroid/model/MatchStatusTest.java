package kittycards.kittycardsandroid.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class MatchStatusTest {

    @Test
    public void matchStatusShouldContainRunning() {
        assertEquals(MatchStatus.RUNNING, MatchStatus.valueOf("RUNNING"));
    }

    @Test
    public void matchStatusShouldContainPaused() {
        assertEquals(MatchStatus.PAUSED, MatchStatus.valueOf("PAUSED"));
    }

    @Test
    public void matchStatusShouldContainFinished() {
        assertEquals(MatchStatus.FINISHED, MatchStatus.valueOf("FINISHED"));
    }

    @Test
    public void matchStatusShouldContainWaitingForNetwork() {
        assertEquals(MatchStatus.WAITING_FOR_NETWORK, MatchStatus.valueOf("WAITING_FOR_NETWORK"));
    }

    @Test
    public void matchStatusShouldContainExactlyFourValues() {
        assertEquals(4, MatchStatus.values().length);
    }
}