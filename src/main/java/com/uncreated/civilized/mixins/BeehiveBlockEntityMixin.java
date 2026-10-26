package com.uncreated.civilized.mixins;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.util.WorkAreaUtil;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.skills.SkillEffects;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkill;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BeehiveBlockEntity.class)
public abstract class BeehiveBlockEntityMixin {

   @WrapOperation(method = "releaseOccupant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
   private static boolean civilized$honeyAccumulationChance(
         Level level,
         BlockPos pos,
         BlockState newHiveState,
         Operation<Boolean> original) {
      if (!(level instanceof ServerLevel serverLevel))
         return original.call(level, pos, newHiveState);

      int honeyChange = getHoneyChange(serverLevel, pos);
      if (honeyChange == 0) // no honey change, simply run the original method
         return original.call(level, pos, newHiveState);

      // the hive hasn't been changed yet, so it's block pos in the world still has the old unchanged value (newHoney is
      // the new state that will be written)
      BlockState currentHiveState = level.getBlockState(pos);
      int currentHoney = currentHiveState.getValue(BeehiveBlock.HONEY_LEVEL);
      int proposedNewHoney = newHiveState.getValue(BeehiveBlock.HONEY_LEVEL);
      int newHoney = Math.min(proposedNewHoney + honeyChange, BeehiveBlock.MAX_HONEY_LEVELS);

      // bee failed to add honey due to the skill effect
      if (newHoney <= currentHoney)
         return false;

      // otherwise, honey must have increased due to the skill effect - call the original method with an altered hive
      // block state
      return original.call(level, pos, newHiveState.setValue(BeehiveBlock.HONEY_LEVEL, newHoney));
   }

   private static int getHoneyChange(ServerLevel level, BlockPos hivePos) {
      Optional<Building> farm = ServerBuildingsStore.INSTANCE.findEnclosingBuilding(hivePos, level);
      if (farm.isEmpty() || !farm.get().getBuildingType().is(BuildingTypes.BEE_FARM))
         return 0;

      int mostSkilledBeekeeperLevel = -1;
      for (VillagerInfo beekeeper : WorkAreaUtil.getWorkers(farm.get(), ServerVillagerStore.INSTANCE))
         mostSkilledBeekeeperLevel =
               Math.max(mostSkilledBeekeeperLevel, beekeeper.getSkills().getLevel(VillagerSkill.ANIMALS));

      // a farm with no assigned workers accumulates honey at normal speed
      if (mostSkilledBeekeeperLevel < 0)
         return 0;

      double honeyAlterChance = SkillEffects.getAlterHoneyAccumulationChance(mostSkilledBeekeeperLevel);
      if (honeyAlterChance == 0)
         return 0;

      double random = level.getRandom().nextDouble();
      if (honeyAlterChance > 0)
         return random < honeyAlterChance ? 1 : 0;
      else
         return random < -honeyAlterChance ? -1 : 0;
   }
}
