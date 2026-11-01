import mindustry.core.GameState.State;
import mindustry.game.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** 波次暂停：每关一次、定时自动继续、暂停期间跳过无效。 */
public class WavePauseTests{

    @BeforeAll
    static void init(){
        ApplicationTests.launchApplication();
    }

    @BeforeEach
    void setup(){
        logic.reset();
        world.loadMap(ApplicationTests.testMap);
        state.set(State.playing);
        state.rules.waves = true;
        state.rules.attackMode = false;
    }

    @Test
    void pauseIsOneShotPerSector(){
        assertTrue(logic.canPauseWave(), "a fresh sector must allow pausing");

        logic.pauseWave(0f);
        assertTrue(state.rules.wavePause, "pauseWave must pause the wave");
        assertTrue(state.rules.wavePauseUsed, "pausing must consume the one-shot");
        assertFalse(logic.canPauseWave(), "a used pause must not be offered again");

        logic.resumeWave();
        assertFalse(state.rules.wavePause, "resumeWave must clear the paused state");
        assertFalse(logic.canPauseWave(), "after resuming, pausing again must be refused");

        logic.pauseWave(600f);
        assertFalse(state.rules.wavePause, "a second pauseWave call must be ignored");
    }

    @Test
    void timedPauseExpiresOnce(){
        logic.pauseWave(10f * 60f);

        logic.updateWavePause(9f * 60f);
        assertTrue(state.rules.wavePause, "pause must still be active before the time is up");

        logic.updateWavePause(61f);
        assertFalse(state.rules.wavePause, "pause must resume automatically when the time is up");
        assertFalse(logic.canPauseWave(), "a timed pause must consume the one-shot");
    }

    @Test
    void pauseRequiresWavesAndNoAttackMode(){
        state.rules.waves = false;
        assertFalse(logic.canPauseWave(), "cannot pause when the sector has no waves");

        state.rules.waves = true;
        state.rules.attackMode = true;
        assertFalse(logic.canPauseWave(), "cannot pause in attack mode");
    }

    @Test
    void skipIsBlockedWhilePaused(){
        int before = state.wave;

        logic.pauseWave(0f);
        logic.skipWave();
        assertEquals(before, state.wave, "skipping while paused must not advance the wave");

        logic.resumeWave();
        logic.skipWave();
        assertEquals(before + 1, state.wave, "skipping after resuming must advance the wave");
    }
}