package com.uncreated.civilized.entity.sensor;

import com.google.common.collect.ImmutableMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.NearestVisibleLivingEntitySensor;

public class CivilizedVillagerEnemySensor extends NearestVisibleLivingEntitySensor {
   private static final ImmutableMap<EntityType<?>, Float> ACCEPTABLE_DISTANCE_FROM_HOSTILES =
         ImmutableMap.<EntityType<?>, Float> builder()
               .put(EntityType.DROWNED, 32.0F)
               .put(EntityType.EVOKER, 32.0F)
               .put(EntityType.HUSK, 32.0F)
               .put(EntityType.ILLUSIONER, 32.0F)
               .put(EntityType.PILLAGER, 32.0F)
               .put(EntityType.RAVAGER, 32.0F)
               .put(EntityType.VINDICATOR, 32.0F)
               .put(EntityType.ZOGLIN, 32.0F)
               .put(EntityType.ZOMBIE, 32.0F)
               .put(EntityType.ZOMBIE_VILLAGER, 32.0F)
               .build();

   protected boolean isMatchingEntity(ServerLevel serverLevel, LivingEntity attacker, LivingEntity target) {
      return this.isHostile(target) && this.isClose(attacker, target);
   }

   private boolean isClose(LivingEntity attacker, LivingEntity target) {
      float f = (Float) ACCEPTABLE_DISTANCE_FROM_HOSTILES.get(target.getType());
      return target.distanceToSqr(attacker) <= (double) (f * f);
   }

   protected MemoryModuleType<LivingEntity> getMemory() {
      return MemoryModuleType.NEAREST_HOSTILE;
   }

   private boolean isHostile(LivingEntity entity) {
      return ACCEPTABLE_DISTANCE_FROM_HOSTILES.containsKey(entity.getType());
   }
}
