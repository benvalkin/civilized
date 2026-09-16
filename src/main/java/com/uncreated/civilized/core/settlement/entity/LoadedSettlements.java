package com.uncreated.civilized.core.settlement.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.entity.CivilizedVillager;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.settlement.Settlement;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class LoadedSettlements {

   private static final Logger LOGGER = LogUtils.getLogger();

   private final static Map<UUID, LoadedSettlement> loadedSettlements = new HashMap<>();

   public static void onBuildingLoaded(Settlement settlement, Level level) {

      Optional<LoadedSettlement> loadedSettlement = checkLoaded(settlement);
      if (loadedSettlement.isEmpty())
         loadedSettlements.put(settlement.getSettlementId(), new LoadedSettlement(settlement, level));
      else
         loadedSettlement.get().incrementLoadedBuildingsCount();
   }

   public static void onBuildingUnloaded(Building building) {
      Optional<LoadedSettlement> loadedSettlement = checkLoaded(building.getSettlementId());
      if (loadedSettlement.isEmpty())
         return;

      loadedSettlement.get().decrementLoadedBuildingsCount();
      if (loadedSettlement.get().getLoadedBuildingsCount() <= 0)
         loadedSettlements.remove(building.getSettlementId());
   }

   public static Optional<LoadedSettlement> checkLoaded(UUID settlementId) {
      return Optional.ofNullable(loadedSettlements.get(settlementId));
   }

   public static Optional<LoadedSettlement> checkLoaded(Settlement settlement) {
      return Optional.ofNullable(loadedSettlements.get(settlement.getSettlementId()));
   }

   public static Optional<LoadedSettlement> checkLoaded(Predicate<LoadedSettlement> settlementSearch) {
      return loadedSettlements.values().stream().filter(settlementSearch).findFirst();
   }

   public static LoadedSettlement getLoaded(Settlement settlement) {
      return checkLoaded(settlement).orElseThrow();
   }

   public static void tickLoadedSettlements() {
      for (LoadedSettlement loadedSettlement : loadedSettlements.values()) {

         try {
            loadedSettlement.getBehaviour()
                  .serverTick((ServerLevel) loadedSettlement.getLevel(), loadedSettlement.getLevel().getGameTime());
         } catch (Exception ex) {
            LOGGER.error("Error while ticking settlement {}", loadedSettlement.getSettlement().getSettlementId(), ex);
         }
      }
   }
}
