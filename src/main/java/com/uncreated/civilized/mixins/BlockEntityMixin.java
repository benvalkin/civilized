package com.uncreated.civilized.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.uncreated.civilized.core.building.entity.LoadedBuildings;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * Keeps each loaded building's list of chests up to date. Minecraft has no event for a block entity being loaded or
 * removed, so the two vanilla methods that cover it are hooked instead: {@code onLoad} runs when a block entity comes
 * to life, i.e. when it is placed or when its chunk loads, and {@code setRemoved} runs when it is broken or its chunk
 * unloads.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {

   @Inject(method = "onLoad", at = @At("TAIL"))
   private void civilized$onChestLoaded(CallbackInfo callbackInfo) {
      BlockEntity blockEntity = (BlockEntity) (Object) this;

      if (!(blockEntity instanceof ChestBlockEntity chest))
         return;

      Level level = blockEntity.getLevel();
      if (level == null || level.isClientSide())
         return;

      LoadedBuildings.onChestLoaded(chest, level);
   }

   @Inject(method = "setRemoved", at = @At("TAIL"))
   private void civilized$onChestUnloaded(CallbackInfo callbackInfo) {
      BlockEntity blockEntity = (BlockEntity) (Object) this;

      if (!(blockEntity instanceof ChestBlockEntity chest))
         return;

      Level level = blockEntity.getLevel();
      if (level == null || level.isClientSide())
         return;

      LoadedBuildings.onChestUnloaded(chest, level);
   }
}
