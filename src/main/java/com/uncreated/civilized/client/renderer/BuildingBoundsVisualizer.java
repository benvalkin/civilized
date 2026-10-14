package com.uncreated.civilized.client.renderer;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uncreated.civilized.networking.packets.TellClientVisualizeBounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = CIVILIZED_MOD_ID)
public final class BuildingBoundsVisualizer {

   private static final long DETECTION_DISPLAY_DURATION_MS = 60_000;

   private record DetectionDisplay(TellClientVisualizeBounds detection, ResourceKey<Level> dimension, long until) {
   }

   private static @Nullable DetectionDisplay detectionDisplay;

   private BuildingBoundsVisualizer() {
   }

   public static void showDetection(TellClientVisualizeBounds detection) {
      Level level = Minecraft.getInstance().level;
      if (detection.isClear() || level == null) {
         detectionDisplay = null;
         return;
      }

      detectionDisplay =
            new DetectionDisplay(
                  detection,
                  level.dimension(),
                  System.currentTimeMillis() + DETECTION_DISPLAY_DURATION_MS);
   }

   public static void drawBox(
         PoseStack poseStack,
         VertexConsumer consumer,
         Vec3 camera,
         BlockPos pos1,
         BlockPos pos2,
         float red,
         float green,
         float blue) {
      drawShape(poseStack, consumer, camera, AABB.encapsulatingFullBlocks(pos1, pos2), red, green, blue);
   }

   private static void drawShape(
         PoseStack poseStack,
         VertexConsumer consumer,
         Vec3 camera,
         AABB shape,
         float red,
         float green,
         float blue) {
      // relative to the camera before going down to floats, so that boxes far from the world's origin stay accurate
      ShapeRenderer.renderLineBox(poseStack, consumer, shape.move(camera.reverse()), red, green, blue, 1F);
   }

   private static void drawMarker(
         PoseStack poseStack,
         VertexConsumer consumer,
         Vec3 camera,
         TellClientVisualizeBounds.Marker marker) {
      BlockPos pos = marker.pos();
      AABB block = new AABB(pos);
      AABB shape = switch (marker.type()) {
      // a flat square on the floor, so a room's floor reads as a grid
      case ENCLOSED_FLOOR,
            UNENCLOSED_FLOOR ->
         new AABB(
               pos.getX() + 0.15,
               pos.getY() + 0.01,
               pos.getZ() + 0.15,
               pos.getX() + 0.85,
               pos.getY() + 0.03,
               pos.getZ() + 0.85);
      case DOOR -> block.deflate(0.05);
      case BOUNDARY -> block.deflate(0.2);
      case START -> AABB.ofSize(pos.getCenter(), 0.3, 0.3, 0.3);
      // a little bigger than the block, so it stands out from the blocks around it
      case LEAK -> block.inflate(0.03);
      };

      int colour = switch (marker.type()) {
      case ENCLOSED_FLOOR -> 0x55FF55;
      case UNENCLOSED_FLOOR -> 0xFFAA00;
      case DOOR -> 0xFFFF55;
      case BOUNDARY -> 0x888888;
      case START -> 0x5555FF;
      case LEAK -> 0xFF2222;
      };

      drawShape(
            poseStack,
            consumer,
            camera,
            shape,
            ARGB.red(colour) / 255F,
            ARGB.green(colour) / 255F,
            ARGB.blue(colour) / 255F);
   }

   @SubscribeEvent
   public static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (detectionDisplay == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
         return;

      Level level = Minecraft.getInstance().level;
      if (level == null || !level.dimension().equals(detectionDisplay.dimension())
            || System.currentTimeMillis() >= detectionDisplay.until()) {
         detectionDisplay = null;
         return;
      }

      PoseStack poseStack = event.getPoseStack();
      Vec3 camera = event.getCamera().getPosition();
      VertexConsumer consumer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());

      TellClientVisualizeBounds detection = detectionDisplay.detection();
      for (TellClientVisualizeBounds.Marker marker : detection.markers())
         drawMarker(poseStack, consumer, camera, marker);

      detection.bounds()
            .ifPresent(
                  bounds -> drawBox(
                        poseStack,
                        consumer,
                        camera,
                        bounds.getLowerCorner(),
                        bounds.getUpperCorner(),
                        1F,
                        1F,
                        1F));
   }

   @SubscribeEvent
   public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
      detectionDisplay = null;
   }
}
