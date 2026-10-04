package com.uncreated.civilized.client;


import com.uncreated.civilized.client.renderer.BuildingBoundsDragTool;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.networking.packets.RequestEstablishBuilding;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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

      return BuildingBoundsDragTool.handleUse(player, level, new BuildingBoundsDragTool.IBoundsDragListener() {
         @Override
         public void onSecondCornerPlaced(BuildingBounds bounds) {
            PacketDistributor.sendToServer(new RequestEstablishBuilding(buildingType, bounds, false));
         }

         @Override
         public void onBoundsConfirmed(BuildingBounds bounds) {
            PacketDistributor.sendToServer(new RequestEstablishBuilding(buildingType, bounds, true));
         }
      });
   }
}
