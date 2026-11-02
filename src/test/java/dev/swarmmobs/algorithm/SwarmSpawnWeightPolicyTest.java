package dev.swarmmobs.algorithm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwarmSpawnWeightPolicyTest {
    @Test void vanillaMultiplierPreservesOriginalWeight() {
        assertEquals(100, SwarmSpawnWeightPolicy.adjustedWeight(100, 1.0));
    }

    @Test void zeroDisablesOnlyTheSelectedNaturalSpawnEntry() {
        assertEquals(0, SwarmSpawnWeightPolicy.adjustedWeight(100, 0.0));
    }

    @Test void multipliersScaleRelativeWeightAndRoundToAnInteger() {
        assertEquals(50, SwarmSpawnWeightPolicy.adjustedWeight(100, 0.5));
        assertEquals(175, SwarmSpawnWeightPolicy.adjustedWeight(100, 1.75));
        assertEquals(200, SwarmSpawnWeightPolicy.adjustedWeight(100, 2.0));
    }

    @Test void invalidAndExtremeInputsAreBounded() {
        assertEquals(100, SwarmSpawnWeightPolicy.adjustedWeight(100, Double.NaN));
        assertEquals(400, SwarmSpawnWeightPolicy.adjustedWeight(100, 99.0));
        assertEquals(0, SwarmSpawnWeightPolicy.adjustedWeight(0, 2.0));
    }
}
