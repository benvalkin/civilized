package com.uncreated.civilized.core.building.requirement;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.item.CurrencyItem;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;

/**
 * Builds {@link RequirementContext}s on the server. Server-side requirements context is able to see stuff that the
 * client may be unable/untrusted to, e.g. checking if the the settlement has enough coins in the town center.
 */
public class ServerRequirementContexts {

   /**
    * @param settlement
    *           the settlement the building belongs to, or null if there isn't one yet (this happens when placing a town
    *           hall)
    * @param player
    *           the player establishing or upgrading the building.
    */
   public static RequirementContext create(
         ServerLevel level,
         BuildingBounds bounds,
         @Nullable Settlement settlement,
         ServerPlayer player) {

      int population = 0;
      int townHallLevel = 0;
      if (settlement != null) {
         population = ServerVillagerStore.INSTANCE.getCitizens(settlement.getSettlementId()).size();
         townHallLevel =
               ServerBuildingsStore.INSTANCE.findTownHall(settlement.getSettlementId())
                     .map(Building::getUpgradeLevel)
                     .orElse(0);
      }

      int availableCurrency = CurrencyItem.countCurrency(coinStorage(settlement, player));
      return new RequirementContext(level, bounds, population, availableCurrency, townHallLevel);
   }

   /**
    * A list of containers (player inventory and building chest) that will be used to debit coins when
    * {@link CurrencyRequirement} are checked and applied. Containers are debited in the order that they appear in the
    * list. This implementation contains the following containers in order: the player's inventory, then the town hall's
    * chests (if the townhall is loaded), then the storehouse's chests (if the storehouse is loaded).
    */
   public static List<Container> coinStorage(@Nullable Settlement settlement, ServerPlayer player) {
      List<Container> storage = new ArrayList<>();
      storage.add(player.getInventory());

      if (settlement != null) {
         ServerBuildingsStore.INSTANCE.findTownHall(settlement.getSettlementId())
               .ifPresent(b -> storage.addAll(loadedChests(b)));
         ServerBuildingsStore.INSTANCE.findStorehouse(settlement.getSettlementId())
               .ifPresent(b -> storage.addAll(loadedChests(b)));
      }

      return storage;
   }

   private static List<Container> loadedChests(Building building) {
      return LoadedBuildings.checkLoaded(building).map(LoadedBuilding::chests).orElse(List.of());
   }
}
