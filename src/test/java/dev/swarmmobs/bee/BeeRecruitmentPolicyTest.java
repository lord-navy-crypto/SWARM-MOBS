package dev.swarmmobs.bee;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeRecruitmentPolicyTest {
    @Test void recruitmentPrefersFreshUsefulPatchesWhenTheHiveNeedsNectar() {
        double highNeed = BeeRecruitmentPolicy.priority(.8, .9, 1.0, .1);
        double lowNeed = BeeRecruitmentPolicy.priority(.8, .9, .1, .1);
        assertTrue(highNeed > lowNeed);
    }

    @Test void congestionAndLowConfidenceReduceRecruitment() {
        double clear = BeeRecruitmentPolicy.priority(.8, .9, 1.0, .1);
        double congested = BeeRecruitmentPolicy.priority(.8, .9, 1.0, .8);
        double uncertain = BeeRecruitmentPolicy.priority(.8, .2, 1.0, .1);
        assertTrue(clear > congested);
        assertTrue(clear > uncertain);
    }

    @Test void negativeUtilityDoesNotRecruit() {
        assertEquals(0.0,
                BeeRecruitmentPolicy.priority(-.5, .9, 1.0, 0.0), 1.0e-9);
    }

    @Test void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> BeeRecruitmentPolicy.priority(Double.NaN, .9, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BeeRecruitmentPolicy.priority(.5, 1.1, 1, 0));
    }
}
