package com.uncreated.civilized.entity.behaviour;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class IdleStrollAroundSettlement extends StatefulBehaviour {
   public static final Logger LOGGER = LogUtils.getLogger();
   /** How many spots inside a building are tried when strolling around inside it. */
   private static final int LINGER_SPOT_ATTEMPTS = 8;
   private final int maxHorizontalDist;
   private final int maxVerticalDist;
   private final float speedModifier;
   private long nextStrollTime;

   public IdleStrollAroundSettlement(int maxHorizontalDist, int maxVerticalDist, float strollSpeedModifier) {
      super(BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT, 5 * 20, 10 * 20, 0);
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
      nextStrollTime = gameTime;
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

      if (tickTime >= nextStrollTime) {
         nextStrollTime += villager.getRandom().nextInt(4 * 20, 12 * 20);

         if (tryStrollInsideLingeringBuilding(level, villager))
            return;

         Vec3 wanderPos;
         Optional<LoadedSettlement> settlement;
         if (villager.getInfo().getSettlementId() == null) {
            wanderPos = LandRandomPos.getPos(villager, maxHorizontalDist, maxVerticalDist);
            settlement = Optional.empty();
         } else {
            settlement = LoadedSettlements.checkLoaded(villager.getInfo().getSettlementId());
            if (settlement.isPresent()
                  && !settlement.get().getSettlement().getBounds().contains(villager.blockPosition())) {
               wanderPos =
                     DefaultRandomPos.getPosTowards(
                           villager,
                           maxHorizontalDist,
                           maxVerticalDist,
                           settlement.get().getSettlement().getBounds().getOrigin().getCenter(),
                           (float) Math.PI / 2F);
            } else {
               wanderPos = LandRandomPos.getPos(villager, maxHorizontalDist, maxVerticalDist);
            }
         }

         if (wanderPos != null) {
            boolean wanderPosInsideBuilding =
                  settlement.isPresent() && settlement.get()
                        .getLoadedBuildings()
                        .stream()
                        .anyMatch(a -> a.getBuilding().getBounds().contains(BlockPos.containing(wanderPos)));

            if (!wanderPosInsideBuilding)
               villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(wanderPos, speedModifier, 2));
         } else
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      }
   }

   private boolean tryStrollInsideLingeringBuilding(ServerLevel level, CivilizedVillager villager) {
      Optional<UUID> buildingId = villager.getBrain().getMemory(AIRegistry.MM_LINGERING_AT_BUILDING.get());
      if (buildingId.isEmpty())
         return false;

      Optional<Building> building = ServerBuildingsStore.INSTANCE.find(buildingId.get());
      if (building.isEmpty() || LoadedBuildings.checkLoaded(building.get()).isEmpty()) {
         villager.getBrain().eraseMemory(AIRegistry.MM_LINGERING_AT_BUILDING.get());
         return false;
      }

      // if no spot is found this time, the villager just stays where it is until the next stroll
      building.get()
            .getBounds()
            .findRandomStandableSpot(level, villager.getRandom(), LINGER_SPOT_ATTEMPTS)
            .ifPresent(
                  spot -> villager.getBrain()
                        .setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot, speedModifier, 1)));
      return true;
   }

   @Override
   public Component description() {
      return Component.empty();
   }
}
