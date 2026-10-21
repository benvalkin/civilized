package com.uncreated.civilized.core.building.util;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingStore;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerStore;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class BuildingUtil {

   public static void serverRefreshAndReplicate(UUID... buildingId) {
      Stream.of(buildingId)
            .filter(Objects::nonNull)
            .distinct() // remove duplicates
            .forEach(b -> {
               Optional<Building> building = ServerBuildingsStore.INSTANCE.find(b);
               if (building.isEmpty())
                  return;

               boolean success = BuildingUtil.refreshOccupantIds(building.get(), ServerVillagerStore.INSTANCE);
               if (!success)
                  return;

               ServerBuildingsStore.INSTANCE.setDirty();
               ServerBuildingsStore.INSTANCE.replicateChange(building.get(), StoreOperation.UPDATE);
            });
   }

   public static List<VillagerInfo> getOccupants(Building building, VillagerStore store) {
      return store.getCitizens(building.getSettlementId()).stream().filter(v -> v.isOccupantOf(building)).toList();
   }

   public static List<VillagerInfo> getVisitors(Building building, VillagerStore store) {
      // visitors don't belong to any settlement, so they're all filed under the null settlement
      return store.getCitizens(null).stream().filter(v -> v.isOccupantOf(building)).toList();
   }

   public static List<VillagerInfo> getAssignedWorkers(Building building, VillagerStore store) {
      return store.getCitizens(building.getSettlementId())
            .stream()
            .filter(v -> v.isAssignedWorkerOf(building))
            .toList();
   }

   public static List<VillagerInfo> getWorkers(Building building, VillagerStore store) {
      return getAssignedWorkers(building, store);
   }

   /**
    * Rebuilds the building's occupant ids from who lives and works there, since they're only a copy of the villagers'
    * home and worksite ids.
    *
    * @return whether the occupant ids changed
    */
   public static boolean refreshOccupantIds(Building building, VillagerStore store) {
      List<UUID> occupantIds =
            store.getCitizens(building.getSettlementId())
                  .stream()
                  .filter(v -> v.isOccupantOf(building) || v.isAssignedWorkerOf(building))
                  .map(VillagerInfo::getVillagerId)
                  .toList();
      if (new HashSet<>(occupantIds).equals(new HashSet<>(building.getOccupantIds())))
         return false;

      building.getOccupantIds().clear();
      building.getOccupantIds().addAll(occupantIds);
      return true;
   }

   public static boolean worksiteHasFreeWorkerSlot(Building building, VillagerStore store) {
      return getAssignedWorkers(building, store).size() < building.getMaxWorkers();
   }

   public static Optional<Building> findUnoccupiedWorksite(
         UUID settlementId,
         Predicate<BuildingType> filter,
         BuildingStore buildingStore,
         VillagerStore villagerStore) {

      return buildingStore.findForSettlement(settlementId)
            .stream()
            .filter(b -> filter.test(b.getBuildingType()) && worksiteHasFreeWorkerSlot(b, villagerStore))
            .findFirst();
   }

   public static Optional<Building> findEmptyGeneralFamilyResidence(
         UUID settlementId,
         BuildingStore buildingStore,
         VillagerStore villagerStore) {

      for (Building building : buildingStore.findForSettlement(settlementId)) {
         BuildingType type = building.getBuildingType();
         if (!type.isFamilyResidence())
            continue;

         if (type.isWorkBuilding()) // explicitly not worker-based residence
            continue;

         if (getOccupants(building, villagerStore).isEmpty())
            return Optional.of(building);
      }

      return Optional.empty();
   }

   public static void ensureBuildingDoorsAreClosed(CivilizedVillager doorCloser, Building building, ServerLevel level) {

      building.getBounds().traverseBlocksWithin(bt -> {
         if (!level.isLoaded(bt.getCurrentBlockPos()))
            return;

         BlockState blockstate = level.getBlockState(bt.getCurrentBlockPos());
         if (blockstate.is(
               BlockTags.MOB_INTERACTABLE_DOORS,
               (blockStateBase) -> blockStateBase.getBlock() instanceof DoorBlock)) {
            DoorBlock doorblock = (DoorBlock) blockstate.getBlock();
            if (doorblock.isOpen(blockstate)) {
               doorblock.setOpen(doorCloser, level, blockstate, bt.getCurrentBlockPos(), false);
            }
         } else if (blockstate
               .is(BlockTags.FENCE_GATES, (blockStateBase) -> blockStateBase.getBlock() instanceof FenceGateBlock)) {
            if (isFenceGateOpen(blockstate)) {
               setOpenFenceGate(level, doorCloser, bt.getCurrentBlockPos(), blockstate, false);
            }
         }
      });
   }

   public static boolean isFenceGateOpen(BlockState blockState) {
      return blockState.getValue(FenceGateBlock.OPEN);
   }

   public static void setOpenFenceGate(
         Level level,
         LivingEntity entity,
         BlockPos blockPos,
         BlockState blockState,
         boolean open) {

      level.setBlockAndUpdate(blockPos, blockState.setValue(FenceGateBlock.OPEN, open));

      FenceGateBlock fenceGateBlock = (FenceGateBlock) blockState.getBlock();

      level.playSound(
            entity,
            blockPos,
            open ? fenceGateBlock.openSound : fenceGateBlock.closeSound,
            SoundSource.BLOCKS,
            1.0F,
            level.getRandom().nextFloat() * 0.1F + 0.9F);

      // lets sculk sensors and wardens hear the villagers opening/closing the gate, like they do with doors.
      // can't think of a situation where this would be important but it's hear anyway
      level.gameEvent(entity, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, blockPos);
   }

   public static List<Container> findChestsInsideBounds(Building building, Level level, boolean checkUnloadedChunks) {
      return building.getBounds()
            .getBlockEntitiesInsideBuilding(level, checkUnloadedChunks)
            .stream()
            .filter(e -> e instanceof ChestBlockEntity)
            .map(e -> ((Container) e))
            .toList();
   }
}
