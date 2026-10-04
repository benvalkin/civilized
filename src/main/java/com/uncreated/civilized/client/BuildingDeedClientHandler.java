package com.uncreated.civilized.client;

import java.util.Optional;

import javax.annotation.Nullable;

import com.uncreated.civilized.client.renderer.BuildingBoundsDragTool;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.ui.menu.building.EstablishBuildingScreen;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Decides what happens when you use a {@link com.uncreated.civilized.item.BuildingDeedItem}. All of the interact logic
 * is client-side - this is why it had to be moved to its own class since it references client-only classes that
 * dedicated servers cannot load.
 */
public final class BuildingDeedClientHandler {

   private BuildingDeedClientHandler() {
   }

   public static InteractionResult use(BuildingType buildingType, Level level, Player player) {

      if (player != Minecraft.getInstance().player)
         return InteractionResult.PASS;

      HitResult hitResult = Minecraft.getInstance().hitResult;
      @Nullable
      BlockPos clickedBlockPos = null;
      @Nullable
      BlockPos clickedAir = null;
      if (hitResult instanceof BlockHitResult blockHitResult && blockHitResult.getType() == HitResult.Type.BLOCK) {
         clickedBlockPos = blockHitResult.getBlockPos();
         clickedAir = clickedBlockPos.mutable().move(blockHitResult.getDirection());
      }

      if (player.isSecondaryUseActive()) {
         BuildingBoundsDragTool.resetDragging();
         player.displayClientMessage(
               Component.translatable("message.building.placement.help.placement_cancelled"),
               true);
         return InteractionResult.SUCCESS;
      }

      if (BuildingBoundsDragTool.isDraggingComplete()) {

         BuildingBoundsDragTool.BuildingBoundsDragResult boundsResult =
               BuildingBoundsDragTool.getBuildingBoundsDragResult(level);

         if (clickedBlockPos == null || boundsResult.bounds().contains(clickedBlockPos)) {
            if (!validateBounds(buildingType, boundsResult, player, level)) {
               return InteractionResult.FAIL;
            }

            // the server is responsible for checking the requirements, but we can open the screen so long
            Minecraft.getInstance().setScreen(new EstablishBuildingScreen(buildingType, boundsResult.bounds()));

            return InteractionResult.SUCCESS;
         }
      }

      if (clickedAir != null) {
         if (!BuildingBoundsDragTool.isBusyDragging()) {
            BuildingBoundsDragTool.startDraggingBounds(clickedAir);
            player.displayClientMessage(
                  Component.translatable("message.building.placement.help.placed_origin")
                        .withColor(Colors.VALIDATION_PARTIAL_SUCCESS),
                  true);
            return InteractionResult.SUCCESS;
         } else if (!BuildingBoundsDragTool.isDraggingComplete()) {
            BuildingBoundsDragTool.completeDragging(clickedAir);

            BuildingBoundsDragTool.BuildingBoundsDragResult boundsResult =
                  BuildingBoundsDragTool.getBuildingBoundsDragResult(level);

            if (!validateBounds(buildingType, boundsResult, player, level)) {
               return InteractionResult.FAIL;
            }

            player.displayClientMessage(
                  Component.translatable("message.building.placement.help.placed_destination")
                        .withColor(Colors.VALIDATION_PARTIAL_SUCCESS),
                  true);

            return InteractionResult.SUCCESS;
         }
      }

      return InteractionResult.PASS;
   }

   private static boolean validateBounds(
         BuildingType buildingType,
         BuildingBoundsDragTool.BuildingBoundsDragResult boundsResult,
         Player player,
         Level level) {
      Optional<Building> overlappingOther =
            ClientBuildingStore.INSTANCE.findOverlappingBuilding(boundsResult.bounds(), level);
      if (overlappingOther.isPresent()) {
         player.displayClientMessage(
               Component
                     .translatable(
                           "message.building.placement.validation.building_overlapping",
                           overlappingOther.get().getBuildingType().translation())
                     .withColor(Colors.VALIDATION_ERROR),
               true);
         return false;
      }

      if (!buildingType.isWorksite()) {
         if (!boundsResult.centerIsAir()) {
            player.displayClientMessage(
                  Component
                        .translatable(
                              "message.building.placement.validation.center_obstructed",
                              boundsResult.bounds().getCenter().toShortString())
                        .withColor(Colors.VALIDATION_ERROR),
                  true);
            return false;
         }

         if (!buildingType.is(BuildingTypes.TOWN_SQUARE) && !boundsResult.centerIsInside()) {
            player.displayClientMessage(
                  Component
                        .translatable(
                              "message.building.placement.validation.center_no_roof",
                              boundsResult.bounds().getCenter().toShortString())
                        .withColor(Colors.VALIDATION_ERROR),
                  true);
            return false;
         }
      }

      return true;
   }
}
