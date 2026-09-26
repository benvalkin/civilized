package com.uncreated.civilized.entity.behaviour;

import java.util.Map;

import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;

public class SpeakToPlayer extends Behavior<CivilizedVillager> {

   public SpeakToPlayer() {
      super(Map.of(AIRegistry.MM_DIALOGUE_TARGET.get(), MemoryStatus.VALUE_PRESENT), 120);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager villager, long gameTime) {
      return villager.getBrain().hasMemoryValue(AIRegistry.MM_DIALOGUE_TARGET.get());
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      Player player = villager.getBrain().getMemory(AIRegistry.MM_DIALOGUE_TARGET.get()).orElseThrow();
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(player, 0.3f, 2));
      villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true));
      villager.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, player);
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      villager.getBrain().eraseMemory(AIRegistry.MM_DIALOGUE_TARGET.get());
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
   }
}
