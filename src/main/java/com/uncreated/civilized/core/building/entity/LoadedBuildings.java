package com.uncreated.civilized.core.building.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.Building;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class LoadedBuildings {

   private static final Logger LOGGER = LogUtils.getLogger();

   private final static Map<UUID, LoadedBuilding> loadedBuildings = new HashMap<>();

   public static LoadedBuilding load(Building building, Level level) {
      LoadedBuilding loadedBuilding = new LoadedBuilding(building, level);
      loadedBuildings.put(building.getBuildingId(), loadedBuilding);
      return loadedBuilding;
   }

   public static Optional<LoadedBuilding> unload(UUID buildingId) {
      return Optional.ofNullable(loadedBuildings.remove(buildingId));
   }

   public static Optional<LoadedBuilding> checkLoaded(UUID buildingId) {
      return Optional.ofNullable(loadedBuildings.get(buildingId));
   }

   public static Optional<LoadedBuilding> checkLoaded(Building building) {
      return Optional.ofNullable(loadedBuildings.get(building.getBuildingId()));
   }

   public static Optional<LoadedBuilding> checkLoaded(Predicate<LoadedBuilding> buildingSearch) {
      return loadedBuildings.values().stream().filter(buildingSearch).findFirst();
   }

   public static LoadedBuilding getLoaded(Building building) {
      return checkLoaded(building).orElseThrow();
   }

   public static void tickLoadedBuildings() {
      for (LoadedBuilding loadedBuilding : loadedBuildings.values()) {

         try {
            loadedBuilding.getBehaviour()
                  .serverTick((ServerLevel) loadedBuilding.getLevel(), loadedBuilding.getLevel().getGameTime());
         } catch (Exception ex) {
            LOGGER.error("Error while ticking building {}", loadedBuilding.getBuilding().getBuildingId(), ex);
         }
      }
   }
}
