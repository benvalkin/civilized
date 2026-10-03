package com.uncreated.civilized.core.building.util;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingStore;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.core.villagerinfo.VillagerStore;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class BuildingUtil {

   public static List<VillagerInfo> getOccupants(Building building, VillagerStore store) {
      return store.getCitizens(building.getSettlementId()).stream().filter(v -> v.isOccupantOf(building)).toList();
   }

   public static List<VillagerInfo> getVisitors(Building building, VillagerStore store) {
      return store.getCitizens(building.getSettlementId())
            .stream()
            .filter(v -> v.getSettlementId() == null && v.isOccupantOf(building))
            .toList();
   }

   public static List<VillagerInfo> getAssignedWorkers(Building building, VillagerStore store) {
      return store.getCitizens(building.getSettlementId())
            .stream()
            .filter(v -> v.isAssignedWorkerOf(building))
            .toList();
   }

   public static boolean workResidenceHasWorker(Building building, VillagerStore store) {
      return getOccupants(building, store).stream()
            .anyMatch(v -> v.getNpcRole().is(VillagerNpcRoles.WORKER) && v.isOccupantOf(building));
   }

   public static boolean worksiteHasAssignedWorker(Building building, VillagerStore store) {
      return store.getCitizens(building.getSettlementId()).stream().anyMatch(v -> v.isAssignedWorkerOf(building));
   }

   public static Optional<Building> findUnoccupiedWorkerHome(
         UUID settlementId,
         BuildingStore buildingStore,
         VillagerStore villagerStore) {

      return buildingStore.findForSettlement(settlementId)
            .stream()
            .filter(
                  b -> b.getBuildingType().isWorkerResidence()
                        && !workResidenceHasWorker(b, villagerStore))
            .findFirst();
   }

   public static Optional<Building> findUnoccupiedWorkerHome(
         UUID settlementId,
         BuildingType requiredBuildingType,
         BuildingStore buildingStore,
         VillagerStore villagerStore) {

      return buildingStore.findForSettlement(settlementId)
            .stream()
            .filter(
                  b -> b.getBuildingType() == requiredBuildingType
                        && !workResidenceHasWorker(b, villagerStore))
            .findFirst();
   }

   public static Optional<Building> findUnoccupiedWorksite(
         UUID settlementId,
         Predicate<BuildingType> filter,
         BuildingStore buildingStore,
         VillagerStore villagerStore) {

      return buildingStore.findForSettlement(settlementId)
            .stream()
            .filter(
                  b -> filter.test(b.getBuildingType())
                        && !worksiteHasAssignedWorker(b, villagerStore))
            .findFirst();
   }

   public static Optional<Building> findEmptyAlternativeHomeForWorker(
           UUID settlementId,
           BuildingStore buildingStore) {

      return buildingStore.findForSettlement(settlementId)
              .stream()
              .filter(
                      b -> b.getBuildingType().is(BuildingTypes.TOWN_HALL)
                              && b.getOccupantIds().isEmpty())
              .findFirst();
   }

   public static Optional<Building> findEmptyAlternativeHomeForSpouse(
           UUID settlementId,
           BuildingStore buildingStore,
           VillagerStore villagerStore) {

      return buildingStore.findForSettlement(settlementId)
              .stream()
              .filter(
                      b -> b.getBuildingType().isSpouseResidence()
                              && b.getOccupantIds().isEmpty())
              .findFirst();
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
}
