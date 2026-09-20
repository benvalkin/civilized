package com.uncreated.civilized.entity.behaviour.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.logistics.hauling.VillagerInventoryType;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.DropOffItemsInstruction;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourState;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import lombok.Getter;
import net.minecraft.server.level.ServerLevel;

public abstract class WorkTaskBehaviour extends StatefulBehaviour {

   private final boolean requiresWorksite;
   private final boolean requiresHome;

   @Getter
   private LoadedSettlement settlement;

   public LoadedBuilding getWorksite() {
      if (!requiresWorksite)
         throw new UnsupportedOperationException(
               "Cannot get villager's worksite. This behaviour is not configured to make use of the villager's worksite ('requiresWorksite' is set to 'false')");

      return worksite;
   }

   public LoadedBuilding getHome() {
      if (!requiresHome)
         throw new UnsupportedOperationException(
               "Cannot get villager's home. This behaviour is not configured to make use of the villager's home ('requiresHome' is set to 'false')");

      return home;
   }

   @Nullable
   protected LoadedBuilding worksite;
   @Nullable
   private LoadedBuilding home;

   public WorkTaskBehaviour(
         BehaviourState state,
         boolean requiresWorksite,
         boolean requiresHome,
         int duration,
         int cooldownDuration) {
      super(state, duration, cooldownDuration);
      this.requiresWorksite = requiresWorksite;
      this.requiresHome = requiresHome;
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {

      Optional<LoadedSettlement> loadedSettlement = LoadedSettlements.checkLoaded(villager.getInfo().getSettlementId());
      if (loadedSettlement.isEmpty())
         return false;

      settlement = loadedSettlement.get();

      if (requiresWorksite) {
         Optional<LoadedBuilding> building = LoadedBuildings.checkLoaded(villager.getInfo().getPrimaryWorksiteId());
         if (building.isEmpty())
            return false;

         worksite = building.get();
      }
      if (requiresHome) {
         Optional<LoadedBuilding> building = LoadedBuildings.checkLoaded(villager.getInfo().getHomeBuildingId());
         if (building.isEmpty())
            return false;

         home = building.get();
      }

      return true;
   }

   protected Optional<LoadedBuilding> findStorehouse(LoadedSettlement settlement) {
      return ServerBuildingsStore.INSTANCE.findStorehouse(settlement.getSettlement().getSettlementId())
            .flatMap(LoadedBuildings::checkLoaded);

   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);
   }

   protected @NotNull List<LoadedBuilding> homeAndStorehouseIfPresent() {
      List<LoadedBuilding> sourceBuildings = new ArrayList<>();
      sourceBuildings.add(getHome());
      findStorehouse(getSettlement()).ifPresent(sourceBuildings::add);
      return sourceBuildings;
   }

   protected void goDropOffWorkOutputAtHome(CivilizedVillager villager) {
      DropOffItemsInstruction dropOffItemsInstruction =
            new DropOffItemsInstruction(getHome(), List.of(VillagerInventoryType.WORK_OUTPUT));
      villager.getBrain().setMemory(AIRegistry.MM_DROP_OFF_ITEMS_INSTRUCTION.get(), dropOffItemsInstruction);
      getStateMachine().queueActionOnce(WorkStates.DROPPING_OFF_ITEMS_AT_BUILDING);
   }
}
