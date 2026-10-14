package com.uncreated.civilized.client.renderer;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.networking.packets.VisualizeBoundsInstruction;

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

   /** What's being shown. */
   public enum Channel {
      /** Result of {@code /detectrooms}. */
      DEBUG_DETECTION,
      /** Preview bounds of a building that the player is busy placing - they are not confirmed yet. */
      PLACEMENT_PREVIEW,
      /** Visualization of problematic bounds (usually leaks etc.). */
      PLACEMENT_PROBLEM
   }

   private static final Duration DEBUG_DETECTION_DURATION = Duration.ofMinutes(1);
   private static final int DEBUG_BOUNDS_COLOUR = 0xFFFFFF;

   private record Display(List<VisualizeBoundsInstruction.Marker> markers, Optional<BuildingBounds> bounds,
                          int boundsColour, ResourceKey<Level> dimension, long untilMillis, BooleanSupplier showWhile) {
   }

   private static final Map<Channel, Display> DISPLAYS = new EnumMap<>(Channel.class);

   private BuildingBoundsVisualizer() {
   }

   public static void show(
         Channel channel,
         List<VisualizeBoundsInstruction.Marker> markers,
         @Nullable BuildingBounds bounds,
         int boundsColour,
         @Nullable Duration duration,
         BooleanSupplier showWhile) {
      Level level = Minecraft.getInstance().level;
      if (level == null)
         return;

      long until = duration == null ? Long.MAX_VALUE : System.currentTimeMillis() + duration.toMillis();
      DISPLAYS.put(
            channel,
            new Display(
                  List.copyOf(markers),
                  Optional.ofNullable(bounds),
                  boundsColour,
                  level.dimension(),
                  until,
                  showWhile));
   }

   public static void hide(Channel channel) {
      DISPLAYS.remove(channel);
   }

   public static void showBoundsFromDebugPacket(VisualizeBoundsInstruction detection) {
      if (detection.isClear()) {
         hide(Channel.DEBUG_DETECTION);
         return;
      }

      show(
            Channel.DEBUG_DETECTION,
            detection.markers(),
            detection.bounds().orElse(null),
            DEBUG_BOUNDS_COLOUR,
            DEBUG_DETECTION_DURATION,
            () -> true);
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
         VisualizeBoundsInstruction.Marker marker) {
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
      if (DISPLAYS.isEmpty() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
         return;

      Level level = Minecraft.getInstance().level;
      if (level == null)
         return;

      long now = System.currentTimeMillis();
      DISPLAYS.values().removeIf(display -> now >= display.untilMillis());

      PoseStack poseStack = event.getPoseStack();
      Vec3 camera = event.getCamera().getPosition();
      VertexConsumer consumer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());

      for (Display display : DISPLAYS.values()) {
         // shown again when the player comes back to its level
         if (!level.dimension().equals(display.dimension()) || !display.showWhile().getAsBoolean())
            continue;

         for (VisualizeBoundsInstruction.Marker marker : display.markers())
            drawMarker(poseStack, consumer, camera, marker);

         int colour = display.boundsColour();
         display.bounds()
               .ifPresent(
                     bounds -> drawBox(
                           poseStack,
                           consumer,
                           camera,
                           bounds.getLowerCorner(),
                           bounds.getUpperCorner(),
                           ARGB.red(colour) / 255F,
                           ARGB.green(colour) / 255F,
                           ARGB.blue(colour) / 255F));
      }
   }

   @SubscribeEvent
   public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
      DISPLAYS.clear();
   }
}
