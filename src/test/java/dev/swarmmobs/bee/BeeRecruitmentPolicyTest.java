package dev.swarmmobs.bee;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeRecruitmentPolicyTest {
    @Test void recruitmentRequiresConfidenceAndNeed() {
        assertEquals(0, BeeRecruitmentPolicy.priority(0.8, 0, 1, 0));
        assertEquals(0, BeeRecruitmentPolicy.priority(0.8, 1, 0, 0));
    }

    @Test void congestionReducesRecruitment() {
        assertTrue(BeeRecruitmentPolicy.priority(0.8, 1, 1, 0.2) >
                   BeeRecruitmentPolicy.priority(0.8, 1, 1, 0.8));
    }
}
