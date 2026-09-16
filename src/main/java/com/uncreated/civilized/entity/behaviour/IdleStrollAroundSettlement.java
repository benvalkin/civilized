package com.uncreated.civilized.entity.behaviour;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class IdleStrollAroundSettlement extends StatefulBehaviour {
   public static final Logger LOGGER = LogUtils.getLogger();
   private final int maxHorizontalDist;
   private final int maxVerticalDist;
   private final float speedModifier;
   private long nextStrolTime;
   private Set<Building> settlementBuildings;
   private Building home;

   public IdleStrollAroundSettlement(int maxHorizontalDist, int maxVerticalDist, float strollSpeedModifier) {
      super(BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT, 20 * 5, 0);
      this.maxHorizontalDist = maxHorizontalDist;
      this.maxVerticalDist = maxVerticalDist;
      this.speedModifier = strollSpeedModifier;
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      return true;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      nextStrolTime = gameTime;
      settlementBuildings = ServerBuildingsStore.INSTANCE.findForSettlement(villager.getInfo().getSettlementId());
      home = ServerBuildingsStore.INSTANCE.find(villager.getInfo().getHomeBuildingId()).orElse(null);
   }

   // @Override
   // protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
   //
   // }

   // @Override
   // protected boolean canStillUse(ServerLevel level, CivilizedVillager villager, long gameTime) {
   // return villager.getBrain().checkMemory(AIRegistry.MM_HAS_NON_IDLE_WORK_TASK.get(), MemoryStatus.VALUE_ABSENT);
   // }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long tickTime) {

      if (tickTime >= nextStrolTime) {
         nextStrolTime += villager.getRandom().nextInt(4 * 20, 12 * 20);

         Vec3 wanderPos;
         if (villager.getInfo().getSettlementId() == null) {
            wanderPos = LandRandomPos.getPos(villager, maxHorizontalDist, maxVerticalDist);
         } else {

            Optional<Settlement> settlement =
                  ServerSettlementsStore.INSTANCE.find(villager.getInfo().getSettlementId());
            if (settlement.isPresent() && !settlement.get().getBounds().contains(villager.blockPosition())) {
               wanderPos =
                     DefaultRandomPos.getPosTowards(
                           villager,
                           maxHorizontalDist,
                           maxVerticalDist,
                           settlement.get().getBounds().getOrigin().getCenter(),
                           (float) Math.PI / 2F);
            } else {
               wanderPos = LandRandomPos.getPos(villager, maxHorizontalDist, maxVerticalDist);
            }
         }

         if (wanderPos != null)
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(wanderPos, speedModifier, 2));
         else
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      }
   }
}
