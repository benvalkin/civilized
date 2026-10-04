package com.uncreated.civilized.client;

import javax.annotation.Nullable;

import com.uncreated.civilized.client.renderer.BuildingBoundsDragTool;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.networking.packets.RequestEstablishBuilding;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;

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

         if (clickedBlockPos != null && boundsResult.bounds().contains(clickedBlockPos)) {
            PacketDistributor.sendToServer(new RequestEstablishBuilding(buildingType, boundsResult.bounds(), true));
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

            // checked straight away, so that bad bounds are rejected (and cleared) before the player tries to confirm
            // them
            BuildingBounds bounds = BuildingBoundsDragTool.getBuildingBoundsDragResult(level).bounds();
            PacketDistributor.sendToServer(new RequestEstablishBuilding(buildingType, bounds, false));

            return InteractionResult.SUCCESS;
         }
      }

      return InteractionResult.PASS;
   }
}
