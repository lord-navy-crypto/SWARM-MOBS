package dev.swarmmobs.algorithm;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SwarmSearchTrailFieldTest {
    private static final UUID TARGET_A = new UUID(1L, 1L);
    private static final UUID TARGET_B = new UUID(2L, 2L);

    @Test
    void marksAreTargetScopedAndSpatiallyLocal() {
        var field = new SwarmSearchTrailField();
        var position = new SwarmSearchTrailField.Position(8, 64, 8);
        assertTrue(field.mark(TARGET_A, position, 100));
        assertTrue(field.coverage(TARGET_A, position, 100) > 0.0);
        assertEquals(0.0, field.coverage(TARGET_B, position, 100), 1.0e-9);
        assertEquals(0.0, field.coverage(TARGET_A,
                new SwarmSearchTrailField.Position(80, 64, 80), 100), 1.0e-9);
    }

    @Test
    void searchTrailDecaysAndEventuallyExpires() {
        var field = new SwarmSearchTrailField();
        var position = new SwarmSearchTrailField.Position(0, 64, 0);
        field.mark(TARGET_A, position, 100);
        double initial = field.coverage(TARGET_A, position, 100);
        double afterHalfLife = field.coverage(TARGET_A, position,
                100 + SwarmSearchTrailField.HALF_LIFE_TICKS);
        assertEquals(initial * 0.5, afterHalfLife, 1.0e-6);
        assertEquals(0.0, field.coverage(TARGET_A, position,
                100 + SwarmSearchTrailField.MAX_AGE_TICKS), 1.0e-9);
        assertEquals(0, field.size(100 + SwarmSearchTrailField.MAX_AGE_TICKS));
    }

    @Test
    void capacityIsBoundedAndInvalidObservationsAreRejected() {
        var field = new SwarmSearchTrailField();
        assertFalse(field.mark(null, new SwarmSearchTrailField.Position(0, 0, 0), 0));
        assertFalse(field.mark(TARGET_A, null, 0));
        assertFalse(field.mark(TARGET_A, new SwarmSearchTrailField.Position(0, 0, 0), -1));
        for (int i = 0; i < SwarmSearchTrailField.MAX_CELLS + 20; i++) {
            assertTrue(field.mark(TARGET_A,
                    new SwarmSearchTrailField.Position(i * 8, 64, 0), i));
        }
        assertTrue(field.size(SwarmSearchTrailField.MAX_CELLS + 30L)
                <= SwarmSearchTrailField.MAX_CELLS);
    }

    @Test
    void verticalBandsDoNotShareSearchCoverage() {
        var field = new SwarmSearchTrailField();
        field.mark(TARGET_A, new SwarmSearchTrailField.Position(0, 20, 0), 10);
        assertEquals(0.0, field.coverage(TARGET_A,
                new SwarmSearchTrailField.Position(0, 80, 0), 10), 1.0e-9);
    }
}
