package com.uncreated.civilized.entity.behaviour.worker.common.logistics;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.logistics.hauling.ReservationKey;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.TransferToBuildingInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.BuildingStockRequirement;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.ItemStockRequirement;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.Cooldowns;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class CheckLogisticsOpportunities extends WorkTaskBehaviour {

   private LoadedBuilding storehouse;

   public CheckLogisticsOpportunities() {
      super(WorkStates.CHECK_LOGISTICS_OPPORTUNITIES, true, 0, 120 * 20);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager entity, long gameTime) {
      return false; // stop immediately (once-off behaviour)
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      return !getBehaviourCooldowns().hasCooldown(Cooldowns.START, level.getGameTime());
   }

   private final BuildingStockRequirement everything =
         new BuildingStockRequirement(
               "export_home_goods_to_storehouse",
               i -> !i.isEmpty(),
               1,
               ItemStockRequirement.UNLIMITED,
               ItemStack.EMPTY);

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {

      getBehaviourCooldowns().startCooldown(Cooldowns.START, Duration.of(30, ChronoUnit.SECONDS), level.getGameTime());

      Optional<LoadedBuilding> storehouse = findStorehouse(getSettlement());
      if (storehouse.isEmpty())
         return;

      this.storehouse = storehouse.get();

      if (!getBehaviourCooldowns().hasCooldown(Cooldowns.EXPORT_RUN, level.getGameTime()) && checkForExportOrders()) {

         String party = reservationPartyKey(villager);
         // note that this instruction reserved every single item in the chest since it takes everything to the
         // storehouse.
         // this seems okay, but maybe double check in future
         Optional<TransferToBuildingInstruction> transferToBuildingInstruction =
               TransferToBuildingInstruction.createIfMetFromSourceBuildings(
                     villager,
                     new ReservationKey(party, "everything"),
                     everything,
                     this.storehouse,
                     List.of(getWorkBuilding()));

         if (transferToBuildingInstruction.isPresent()) {
            getBehaviourCooldowns()
                  .startCooldown(Cooldowns.EXPORT_RUN, Duration.of(2, ChronoUnit.MINUTES), level.getGameTime());

            tryDelegateToSpouse(villager, (v, s) -> {
               v.getBrain().setMemory(AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get(), transferToBuildingInstruction);
               s.queueActionOnce(WorkStates.TAKING_ITEMS_TO_INVENTORY);
            });
         }
      }
   }

   private boolean checkForExportOrders() {
      return getWorkBuilding().chests().stream().anyMatch(c -> !c.isEmpty());
   }
}
