package dev.swarmmobs.visual;

/** Stable event names shared by deterministic state transitions and GeckoLib controllers. */
public enum SwarmAnimationCue {
    ENGINEER_INSPECT("engineer_inspect"),
    ENGINEER_WORK("engineer_work"),
    ENGINEER_COMPLETE("engineer_complete"),
    ENGINEER_CANCEL("engineer_cancel"),
    CARRIER_TRACK("carrier_track"),
    CARRIER_ACQUIRE("carrier_acquire"),
    CARRIER_CARRY("carrier_carry"),
    CARRIER_DELIVER("carrier_deliver"),
    SCOUT_OBSERVE("scout_observe"),
    SCOUT_REPORT("scout_report"),
    SCOUT_ACKNOWLEDGE("scout_acknowledge"),
    FLANK_SETUP("flank_setup"),
    REGROUP("regroup"),
    RELIEF_ROTATION("relief_rotation"),
    ROLE_CHANGE("role_change"),
    SUPPORT_READY("support_ready"),
    BREACH_READY("breach_ready"),
    COMMUNICATION_ACKNOWLEDGED("communication_acknowledged"),
    BEE_FORAGE_DUTY("bee_forage_duty"),
    BEE_FORAGE_DISPATCH("bee_forage_dispatch"),
    BEE_POLLINATE("bee_pollinate"),
    BEE_NURSE_DUTY("bee_nurse_duty"),
    BEE_GUARD_DUTY("bee_guard_duty"),
    BEE_GUARD_ALERT("bee_guard_alert"),
    BEE_BUILD_DUTY("bee_build_duty"),
    BEE_BUILD_COMPLETE("bee_build_complete"),
    BEE_REST("bee_rest"),
    BEE_RETURN_HOME("bee_return_home"),
    BEE_HONEY_DELIVERY("bee_honey_delivery"),
    BEE_SCOUT_REPORT("bee_scout_report");

    private final String animationName;
    SwarmAnimationCue(String animationName) { this.animationName = animationName; }
    public String animationName() { return animationName; }
}
