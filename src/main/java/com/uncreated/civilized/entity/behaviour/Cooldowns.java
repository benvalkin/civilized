package com.uncreated.civilized.entity.behaviour;

public class Cooldowns {
    public static final Cooldown START =  new Cooldown("start");
    public static final Cooldown CHECK_START =  new Cooldown("check_start");
    public static final Cooldown EXPORT_RUN =  new Cooldown("export_run");
    public static final Cooldown IMPORT_RUN =  new Cooldown("import_run");
    /** Waiting for a spouse to bring over the items the villager asked for, rather than fetching them again. */
    public static final Cooldown AWAITING_DELIVERY =  new Cooldown("awaiting_delivery");
}
