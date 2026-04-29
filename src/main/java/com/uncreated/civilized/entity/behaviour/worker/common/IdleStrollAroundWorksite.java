package com.uncreated.civilized.entity.behaviour.worker.common;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class IdleStrollAroundWorksite extends WorkTaskBehaviour {
   public static final Logger LOGGER = LogUtils.getLogger();
   private final int maxHorizontalDist;
   private final int maxVerticalDist;
   private final float speedModifier;
   private long nextWorkTime;
   private MediumDistanceTravelTask travelHelper;

   public IdleStrollAroundWorksite(int maxHorizontalDist, int maxVerticalDist, float strollSpeedModifier) {
      super(WorkStates.STROLL_AROUND_WORKSITE, true, false, 120 * 15, 0);
      this.maxHorizontalDist = maxHorizontalDist;
      this.maxVerticalDist = maxVerticalDist;
      this.speedModifier = strollSpeedModifier;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      nextWorkTime = gameTime;
      travelHelper =
            new MediumDistanceTravelTask(villager, getWorksite().getBuilding().getBlockPos(), maxHorizontalDist + 1);
   }

   // @Override
   // protected boolean canStillUse(ServerLevel level, CivilizedVillager villager, long gameTime) {
   // return villager.getBrain().checkMemory(AIRegistry.MM_HAS_NON_IDLE_WORK_TASK.get(), MemoryStatus.VALUE_ABSENT);
   // }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long tickTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(tickTime);
         return;
      }

      if (tickTime >= nextWorkTime) {
         nextWorkTime += villager.getRandom().nextInt(5 * 20, 15 * 20);

         Vec3 wanderPos;
         if (getWorksite().getBuilding().getBounds().contains(villager.blockPosition())) {
            wanderPos = LandRandomPos.getPos(villager, maxHorizontalDist, maxVerticalDist);
         } else {
            wanderPos =
                  LandRandomPos.getPosTowards(
                        villager,
                        maxHorizontalDist,
                        maxVerticalDist,
                        getWorksite().getBuilding().getBlockPos().getBottomCenter());
         }

         if (wanderPos != null)
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(wanderPos, speedModifier, 2));
         else
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      }
   }
}
