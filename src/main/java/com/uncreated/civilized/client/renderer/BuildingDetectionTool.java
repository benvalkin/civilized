package com.uncreated.civilized.client.renderer;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.placement.BuildingPlacement;
import com.uncreated.civilized.item.BuildingDeedItem;
import com.uncreated.civilized.networking.packets.BuildingDetected;
import com.uncreated.civilized.networking.packets.RequestEstablishBuilding;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(value = Dist.CLIENT, modid = CIVILIZED_MOD_ID)
public final class BuildingDetectionTool {

   private static final long HIGHLIGHT_DURATION_MS = 10_000;

   private record Preview(BuildingType buildingType, BlockPos start, BuildingBounds bounds) {
   }

   private static @Nullable Preview preview;
   private static @Nullable BlockPos highlight;
   private static long highlightUntil;

   private BuildingDetectionTool() {
   }

   public static InteractionResult handleUse(Player player, BuildingType buildingType) {
      if (player.isSecondaryUseActive()) {
         reset();
         player.displayClientMessage(
               Component.translatable("message.building.placement.help.placement_cancelled"),
               true);
         return InteractionResult.SUCCESS;
      }

      if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
         return InteractionResult.PASS;

      BlockPos clicked = hit.getBlockPos();
      if (preview != null && preview.buildingType() == buildingType && preview.bounds().contains(clicked)) {
         PacketDistributor.sendToServer(
               new RequestEstablishBuilding(buildingType, new BuildingPlacement.Detected(preview.start()), true));
         return InteractionResult.SUCCESS;
      }

      // air block above clicked pos
      BlockPos start = clicked.relative(hit.getDirection());
      highlight = null;
      PacketDistributor
            .sendToServer(new RequestEstablishBuilding(buildingType, new BuildingPlacement.Detected(start), false));
      return InteractionResult.SUCCESS;
   }

   public static void receiveDetected(BuildingDetected packet) {
      preview = new Preview(packet.buildingType(), packet.buildingOrigin(), packet.bounds());
      highlight = null;

      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null)
         return;

      player.displayClientMessage(
            Component.translatable("message.building.placement.help.detected", packet.buildingType().translation())
                  .withColor(Colors.VALIDATION_PARTIAL_SUCCESS),
            true);
   }

   public static void receiveRejected(@Nullable BlockPos highlight) {
      preview = null;
      BuildingDetectionTool.highlight = highlight;
      highlightUntil = System.currentTimeMillis() + HIGHLIGHT_DURATION_MS;
   }

   public static void reset() {
      preview = null;
      highlight = null;
   }

   @SubscribeEvent
   public static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
         return;

      boolean showPreview = preview != null && isClientPlayerHoldingDeed(preview.buildingType());
      boolean showHighlight = highlight != null && System.currentTimeMillis() < highlightUntil;
      if (!showPreview && !showHighlight)
         return;

      PoseStack poseStack = event.getPoseStack();
      Vec3 camera = event.getCamera().getPosition();
      VertexConsumer consumer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());

      // the same colour as dragged bounds
      if (showPreview)
         BuildingBoundsDragTool.drawBox(
               poseStack,
               consumer,
               camera,
               preview.bounds().getLowerCorner(),
               preview.bounds().getUpperCorner(),
               0.67F,
               0.85F,
               0.96F);

      if (showHighlight)
         BuildingBoundsDragTool.drawBox(poseStack, consumer, camera, highlight, highlight, 1.0F, 0.2F, 0.2F);
   }

   private static boolean isClientPlayerHoldingDeed(BuildingType buildingType) {
      LocalPlayer player = Minecraft.getInstance().player;
      return player != null && player.getMainHandItem().getItem() instanceof BuildingDeedItem deed
            && deed.getBuildingType() == buildingType;
   }

   @SubscribeEvent
   public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
      reset();
   }
}
