package com.uncreated.civilized.entity.behaviour;

import java.util.Map;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public abstract class RecurringIntervalBehaviour<T extends LivingEntity> extends Behavior<T> {

   private long timeStart = -1;

   public RecurringIntervalBehaviour(Map<MemoryModuleType<?>, MemoryStatus> entryCondition) {
      super(entryCondition);
   }

   protected abstract long getIntervalDurationSeconds();

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, T entity) {
      return level.getGameTime() - timeStart > 20 * getIntervalDurationSeconds();
   }

   @Override
   protected void start(ServerLevel level, T entity, long gameTicks) {
      timeStart = gameTicks;
   }
}
