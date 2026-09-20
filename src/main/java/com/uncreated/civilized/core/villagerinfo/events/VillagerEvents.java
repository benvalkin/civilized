package com.uncreated.civilized.core.villagerinfo.events;

import com.uncreated.civilized.entity.CivilizedVillager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;

@EventBusSubscriber
public class VillagerEvents
{
    @SubscribeEvent
    public static void civilizedVillagerKeepSleeping(CanContinueSleepingEvent event) {
        // villagers without a free bed sleep on the floor, which vanilla would otherwise wake them up from straight away
        if (event.getEntity() instanceof CivilizedVillager villager && villager.isSleepingOnFloor())
            event.setContinueSleeping(true);
    }
}
