package com.uncreated.civilized.client;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uncreated.civilized.client.renderer.BuildingBoundsDragTool;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.networking.packets.RequestRedrawBuilding;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Lets the player drag out new bounds for an existing building without holding a building deed item.
 */
// todo: we should probably make the building dead use this system as well
@EventBusSubscriber(value = Dist.CLIENT, modid = CIVILIZED_MOD_ID)
public final class BuildingRedrawSession {

   private static @Nullable UUID buildingId;
   private static @Nullable BuildingBounds currentBounds;

   private BuildingRedrawSession() {
   }

   public static boolean isActive() {
      return buildingId != null;
   }

   public static void start(Building building) {
      buildingId = building.getBuildingId();
      currentBounds = building.getBounds();
      BuildingBoundsDragTool.beginExternalDrag();

      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null)
         player.displayClientMessage(
               Component.translatable("message.building.redraw.help.started", building.getBuildingType().translation())
                     .withColor(Colors.VALIDATION_PARTIAL_SUCCESS),
               true);
   }

   /** Closes the building's menu so that the player can start dragging out its new bounds. */
   public static void startFromMenu(Building building) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null)
         player.closeContainer();

      start(building);
   }

   public static void end() {
      if (!isActive())
         return;

      buildingId = null;
      currentBounds = null;
      BuildingBoundsDragTool.endExternalDrag();
   }

   @SubscribeEvent
   public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
      if (!isActive() || !event.isUseItem())
         return;

      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null)
         return;

      // right-clicks only drag out the bounds while redrawing, rather than placing blocks or using items
      event.setCanceled(true);
      event.setSwingHand(false);

      UUID redrawnBuildingId = buildingId;
      BuildingBoundsDragTool.handleUse(player, player.level(), new BuildingBoundsDragTool.IBoundsDragListener() {
         @Override
         public void onSecondCornerPlaced(BuildingBounds bounds) {
            PacketDistributor.sendToServer(new RequestRedrawBuilding(redrawnBuildingId, bounds, false));
         }

         @Override
         public void onBoundsConfirmed(BuildingBounds bounds) {
            PacketDistributor.sendToServer(new RequestRedrawBuilding(redrawnBuildingId, bounds, true));
         }

         @Override
         public void onCancelled() {
            end();
         }
      });
   }

   @SubscribeEvent
   public static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (currentBounds == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
         return;

      VertexConsumer consumer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());
      // drawn in a different colour to the bounds being dragged out, so the two can be told apart
      BuildingBoundsDragTool.drawBox(
            event.getPoseStack(),
            consumer,
            event.getCamera().getPosition(),
            currentBounds.getLowerCorner(),
            currentBounds.getUpperCorner(),
            0.96F,
            0.75F,
            0.4F);
   }

   @SubscribeEvent
   public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
      end();
   }
}
