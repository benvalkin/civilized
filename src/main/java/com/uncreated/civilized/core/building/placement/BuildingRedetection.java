package com.uncreated.civilized.core.building.placement;

import java.util.Optional;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Auto-detects an existing building's bounds again to ensure that they are still valid. Has no effect on buildings with
 * dragged bounds.
 */
public final class BuildingRedetection {

   private BuildingRedetection() {
   }

   public sealed interface Result {
   }

   public record Unchanged() implements Result {
   }

   /** The building has grown or shrunk but is still valid. */
   public record Changed(BuildingBounds bounds) implements Result {
   }

   /** The building is no longer valid. */
   public record Failed(PlacementResult.Failure failure) implements Result {
   }

   public static Result redetect(ServerLevel level, Building building) {
      if (!building.getBuildingType().detectionType().autoDetected())
         return new Unchanged();

      // where the building was first clicked, which detection started from back then too
      BlockPos start = building.getBounds().getCenter();
      if (!level.isLoaded(start))
         return new Unchanged();

      return switch (BuildingRoomDetection.detect(level, building.getBuildingType(), start)) {
      case BuildingRoomDetection.Rejected rejected -> new Failed(rejected.failure());
      case BuildingRoomDetection.Detected detected -> {
         BuildingBounds newBounds = detected.bounds();
         if (sameBounds(newBounds, building.getBounds()))
            yield new Unchanged();

         Optional<PlacementResult.Failure> invalid = ServerPlacementChecks.checkResize(level, building, newBounds);
         yield invalid.<Result> map(Failed::new).orElseGet(() -> new Changed(newBounds));
      }
      };
   }

   public static BuildingBounds currentBounds(ServerLevel level, Building building) {
      return redetect(level, building) instanceof Changed changed ? changed.bounds() : building.getBounds();
   }

  public static void applyBounds(ServerLevel level, Building building, BuildingBounds bounds) {
      Optional<Settlement> settlement = ServerSettlementsStore.INSTANCE.find(building.getSettlementId());
      if (settlement.isEmpty())
         return;

      // the building may be in different chunks now, so it's loaded again from scratch
      LoadedBuildings.checkLoaded(building).ifPresent(LoadedSettlements::onBuildingUnloaded);
      LoadedBuildings.unload(building.getBuildingId());

      ServerBuildingsStore.INSTANCE.changeBounds(building, bounds);

      if (level.isLoaded(building.getBlockPos())) {
         LoadedBuilding loadedBuilding = LoadedBuildings.load(building, level);
         LoadedSettlements.onBuildingLoaded(settlement.get(), loadedBuilding, level);
      }

      settlement.get()
            .recalculateSettlementBounds(
                  settlement.get().getBounds().getOrigin(),
                  ServerBuildingsStore.INSTANCE.findForSettlement(settlement.get().getSettlementId()));
      ServerSettlementsStore.INSTANCE.setDirty();
      ServerSettlementsStore.INSTANCE.replicateChange(settlement.get(), StoreOperation.ADD_OR_OVERWRITE);

      ServerBuildingsStore.INSTANCE.replicateChange(building, StoreOperation.UPDATE);
   }

   private static boolean sameBounds(BuildingBounds a, BuildingBounds b) {
      return a.getCenter().equals(b.getCenter()) && a.getLowerCorner().equals(b.getLowerCorner())
            && a.getUpperCorner().equals(b.getUpperCorner());
   }
}
