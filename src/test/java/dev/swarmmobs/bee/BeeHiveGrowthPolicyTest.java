package dev.swarmmobs.bee;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeHiveGrowthPolicyTest {
    @Test void expansionRequiresFullConnectedHousingAndHoneyAboveReserve() {
        assertTrue(BeeHiveGrowthPolicy.canExpand(new BeeHiveGrowthPolicy.State(6,6,8,6,2,2,1,0,8)));
        assertFalse(BeeHiveGrowthPolicy.canExpand(new BeeHiveGrowthPolicy.State(5,6,8,6,2,2,1,0,8)));
        assertFalse(BeeHiveGrowthPolicy.canExpand(new BeeHiveGrowthPolicy.State(6,6,6,6,2,2,1,0,8)));
    }
    @Test void expansionRequiresHoneycombAndARealFreeSite() {
        assertFalse(BeeHiveGrowthPolicy.canExpand(new BeeHiveGrowthPolicy.State(6,6,8,6,1,2,1,0,8)));
        assertFalse(BeeHiveGrowthPolicy.canExpand(new BeeHiveGrowthPolicy.State(6,6,8,6,2,2,0,0,8)));
    }
    @Test void expansionStopsAtConfiguredConnectedHiveLimit() {
        assertFalse(BeeHiveGrowthPolicy.canExpand(new BeeHiveGrowthPolicy.State(27,27,30,27,2,2,1,8,8)));
    }
    @Test void invalidGrowthInputsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new BeeHiveGrowthPolicy.State(-1,6,8,6,2,2,1,0,8));
        assertThrows(IllegalArgumentException.class,()->new BeeHiveGrowthPolicy.State(6,6,8,6,2,2,1,9,8));
    }
    @Test void siteScoreRewardsFlowersAndPenalizesCrowdingAndThreats() {
        double safe=BeeHiveGrowthPolicy.expansionSiteScore(new BeeHiveGrowthPolicy.SiteSignals(8,0,0));
        double crowded=BeeHiveGrowthPolicy.expansionSiteScore(new BeeHiveGrowthPolicy.SiteSignals(8,4,3));
        assertTrue(safe>crowded);
        assertEquals(1.0,safe,1.0e-12);
    }
}
