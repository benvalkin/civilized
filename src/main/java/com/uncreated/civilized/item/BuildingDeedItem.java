package com.uncreated.civilized.item;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.uncreated.civilized.client.renderer.BuildingBoundsDragTool;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.building.requirement.EnclosedRoomRequirement;
import com.uncreated.civilized.core.building.requirement.FishingSiteWaterRequirement;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirement;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirementResult;
import com.uncreated.civilized.core.building.requirement.SpaceRequirement;
import com.uncreated.civilized.core.building.requirement.SurfaceAreaRequirement;
import com.uncreated.civilized.core.building.requirement.blockcount.BlockCountRequirement;
import com.uncreated.civilized.core.building.requirement.registry.BuildingRequirementList;
import com.uncreated.civilized.core.building.requirement.registry.BuildingRequirements;
import com.uncreated.civilized.ui.menu.building.EstablishBuildingScreen;
import com.uncreated.civilized.ui.style.Colors;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BuildingDeedItem extends Item {

   @Getter
   private final BuildingType buildingType;

   public BuildingDeedItem(Properties properties, BuildingType buildingType) {
      super(properties);
      this.buildingType = buildingType;
   }

   @Override
   public InteractionResult use(Level level, Player player, InteractionHand hand) {

      if (!level.isClientSide || player != Minecraft.getInstance().player)
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
            if (!validateBounds(boundsResult, player, level)) {
               return InteractionResult.FAIL;
            }

            int buildingLevel = 1;
            BuildingRequirementList requirements =
                  BuildingRequirements.getBuildingRequirements(buildingType, buildingLevel);
            List<IBuildingRequirementResult> requirementResults = Lists.newArrayList();

            Set<BlockPos> validFloorBlocks = Set.of();
            for (IBuildingRequirement requirement : requirements) {

               if (requirement instanceof SpaceRequirement s) {
                  SpaceRequirement.Result spaceResult = s.getResult(level, boundsResult.bounds());
                  validFloorBlocks = spaceResult.getValidFloorBlocks();
                  requirementResults.add(spaceResult);
                  // BAD IMPLEMENTATION: dependant requirements mean that they are also dependent on the order they are
                  // defined in.
                  // EnclosedWallsRequirements will not work if it comes before SpaceRequirement in the list.
               }
               if (requirement instanceof EnclosedRoomRequirement ew)
                  requirementResults.add(ew.getResult(level, boundsResult.bounds()));
               if (requirement instanceof BlockCountRequirement bt)
                  requirementResults.add(bt.getResult(level, boundsResult.bounds()));
               if (requirement instanceof SurfaceAreaRequirement sa)
                  requirementResults.add(sa.getResult(boundsResult.bounds()));
               if (requirement instanceof FishingSiteWaterRequirement fw)
                  requirementResults.add(fw.getResult(level, boundsResult.bounds()));
            }

            Minecraft.getInstance()
                  .setScreen(new EstablishBuildingScreen(buildingType, boundsResult.bounds(), requirementResults));

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

            if (!validateBounds(boundsResult, player, level)) {
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

   private boolean validateBounds(
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

         if (!boundsResult.centerIsInside()) {
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
