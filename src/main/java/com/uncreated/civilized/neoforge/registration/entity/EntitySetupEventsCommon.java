package com.uncreated.civilized.neoforge.registration.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public class EntitySetupEventsCommon {

   @SubscribeEvent
   public static void createDefaultAttributes(EntityAttributeCreationEvent event) {
      event.put(
            EntityRegistry.CIVILIZED_VILLAGER.get(),
            Mob.createMobAttributes().add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.ATTACK_DAMAGE).build());
   }
}
