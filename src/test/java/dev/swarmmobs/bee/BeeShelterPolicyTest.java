package dev.swarmmobs.bee;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeShelterPolicyTest {
    private static final UUID BEE = new UUID(0, 42);

    @Test void unladenBeeMayReturnHomeWhenARealHiveAlarmIsActive() {
        var alert = new BeeHiveAlarmMemory.Alert(
                new UUID(0, 1), 123, 100, 3);
        assertEquals(BeeShelterPolicy.Mode.RETURN_TO_HIVE,
                BeeShelterPolicy.choose(alert, BEE, true,
                        false, false, false, 8.0));
    }

    @Test void nectarCarriersKeepVanillaReturnToHiveBehavior() {
        var alert = new BeeHiveAlarmMemory.Alert(
                new UUID(0, 1), 123, 100, 3);
        assertEquals(BeeShelterPolicy.Mode.NORMAL,
                BeeShelterPolicy.choose(alert, BEE, true,
                        true, false, false, 8.0));
    }

    @Test void stungOrAlreadyTargetingBeesAreNotOverridden() {
        var alert = new BeeHiveAlarmMemory.Alert(
                new UUID(0, 1), 123, 100, 3);
        assertEquals(BeeShelterPolicy.Mode.NORMAL,
                BeeShelterPolicy.choose(alert, BEE, true,
                        false, true, false, 8.0));
        assertEquals(BeeShelterPolicy.Mode.NORMAL,
                BeeShelterPolicy.choose(alert, BEE, true,
                        false, false, true, 8.0));
    }

    @Test void invalidOrDistantBeeDoesNotReceiveShelterCommand() {
        var alert = new BeeHiveAlarmMemory.Alert(
                new UUID(0, 1), 123, 100, 3);
        assertEquals(BeeShelterPolicy.Mode.NORMAL,
                BeeShelterPolicy.choose(alert, BEE, false,
                        false, false, false, 8.0));
        assertEquals(BeeShelterPolicy.Mode.NORMAL,
                BeeShelterPolicy.choose(alert, BEE, true,
                        false, false, false, 25.0));
    }

    @Test void shelterFractionIncreasesWithAlarmSeverity() {
        int severityOne = 0, severityTwo = 0, severityThree = 0;
        for (int i = 0; i < 100; i++) {
            UUID id = new UUID(1, i);
            var one = new BeeHiveAlarmMemory.Alert(new UUID(0, 1), 1, 1, 1);
            var two = new BeeHiveAlarmMemory.Alert(new UUID(0, 1), 1, 1, 2);
            var three = new BeeHiveAlarmMemory.Alert(new UUID(0, 1), 1, 1, 3);
            if (BeeShelterPolicy.choose(one, id, true, false, false, false, 10)
                    == BeeShelterPolicy.Mode.RETURN_TO_HIVE) severityOne++;
            if (BeeShelterPolicy.choose(two, id, true, false, false, false, 10)
                    == BeeShelterPolicy.Mode.RETURN_TO_HIVE) severityTwo++;
            if (BeeShelterPolicy.choose(three, id, true, false, false, false, 10)
                    == BeeShelterPolicy.Mode.RETURN_TO_HIVE) severityThree++;
        }
        assertTrue(severityOne < severityTwo);
        assertTrue(severityTwo < severityThree);
        assertEquals(100, severityThree);
    }
}
