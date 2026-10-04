package com.uncreated.civilized.client.renderer;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.item.BuildingDeedItem;
import com.uncreated.civilized.item.events.EquipmentChange;
import com.uncreated.civilized.item.events.PlayerChangedEquipment;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(value = { Dist.CLIENT }, modid = CIVILIZED_MOD_ID)
public final class BuildingBoundsDragTool {

   public static final int DRAG_LOWER_BOUND = 4;
   public static final int DRAG_HEIGHT_BOUND = 14;
   private static @Nullable ItemStack currentItem;
   private static @Nullable BlockPos origin;
   private static @Nullable BlockPos candidateDestination;
   private static @Nullable BlockPos finalDestination;
   private static @Nullable Player player;

   private static boolean showDraggedBounds;
   /** true if dragging while not holding a building deed item. */
   private static boolean draggingExternally;

   private static final Map<ItemStack, SavedDraggedBounds> savedDraggedBounds = new HashMap<>();

   private BuildingBoundsDragTool() {
   }

   public interface IBoundsDragListener {

      void onSecondCornerPlaced(BuildingBounds bounds);

      void onBoundsConfirmed(BuildingBounds bounds);

      default void onCancelled() {
      }
   }

   /**
    * Handles the player right-clicking while dragging bounds: first click places first corner, second places second
    * corner other, third click confirms bounds placement if clicked pos is inside the box. Shift + Right-click cancels.
    */
   public static InteractionResult handleUse(Player player, Level level, IBoundsDragListener listener) {
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
         resetDragging();
         player.displayClientMessage(
               Component.translatable("message.building.placement.help.placement_cancelled"),
               true);
         listener.onCancelled();
         return InteractionResult.SUCCESS;
      }

      if (isDraggingComplete()) {
         BuildingBounds bounds = getBuildingBoundsDragResult(level).bounds();
         if (clickedBlockPos != null && bounds.contains(clickedBlockPos)) {
            listener.onBoundsConfirmed(bounds);
            return InteractionResult.SUCCESS;
         }
      }

      if (clickedAir == null)
         return InteractionResult.PASS;

      if (!isBusyDragging()) {
         startDraggingBounds(clickedAir);
         player.displayClientMessage(
               Component.translatable("message.building.placement.help.placed_origin")
                     .withColor(Colors.VALIDATION_PARTIAL_SUCCESS),
               true);
         return InteractionResult.SUCCESS;
      }

      if (!isDraggingComplete()) {
         completeDragging(clickedAir);
         // the bounds are checked straight away, so that bad bounds are rejected (and cleared) before the player tries
         // to confirm them
         listener.onSecondCornerPlaced(getBuildingBoundsDragResult(level).bounds());
         return InteractionResult.SUCCESS;
      }

      return InteractionResult.PASS;
   }

   /**
    * Starts dragging bounds without a deed in hand, e.g. to redraw a building. A held deed's bounds are put aside until
    * {@link #endExternalDrag()}.
    */
   public static void beginExternalDrag() {
      if (currentItem != null)
         saveAndHideDraggedBounds(currentItem);

      resetDragging();
      player = Minecraft.getInstance().player;
      draggingExternally = true;
      showDraggedBounds = true;
   }

   public static void endExternalDrag() {
      resetDragging();
      draggingExternally = false;
      showDraggedBounds = false;

      // brings back the bounds of a deed the player is still holding
      Player localPlayer = Minecraft.getInstance().player;
      if (localPlayer != null && localPlayer.getMainHandItem().getItem() instanceof BuildingDeedItem)
         loadAndShowDraggedBounds(localPlayer.getMainHandItem());
   }

   public static void startDraggingBounds(BlockPos origin) {
      BuildingBoundsDragTool.origin = adjustOrigin(origin);
      BuildingBoundsDragTool.candidateDestination = null;
      BuildingBoundsDragTool.finalDestination = null;
   }

   public static void completeDragging(BlockPos destination) {
      if (origin == null)
         return;

      BuildingBoundsDragTool.candidateDestination = null;
      BuildingBoundsDragTool.finalDestination = adjustDestination(destination);
   }

   public static void saveAndHideDraggedBounds(ItemStack buildingDeed) {
      player = Minecraft.getInstance().player;
      showDraggedBounds = false;
      currentItem = null;

      if (origin == null || finalDestination == null)
         return;

      savedDraggedBounds.put(buildingDeed, new SavedDraggedBounds(origin, finalDestination));
      origin = null;
      candidateDestination = null;
      finalDestination = null;
   }

   public static void loadAndShowDraggedBounds(ItemStack buildingDeed) {
      player = Minecraft.getInstance().player;
      showDraggedBounds = true;
      currentItem = buildingDeed;

      @Nullable
      SavedDraggedBounds saved = savedDraggedBounds.remove(buildingDeed);
      if (saved == null)
         return;

      origin = saved.origin();
      finalDestination = saved.finalDestination();
   }

   /**
    * Adjusts the origin block position a bit lower to allow for buildings with basements.
    */
   private static BlockPos adjustOrigin(BlockPos origin) {
      return origin.mutable().move(Direction.DOWN, 4);
   }

   /**
    * Adjusts the destination block position higher to give buildings sufficient height.
    */
   private static BlockPos adjustDestination(BlockPos destination) {
      if (origin == null)
         throw new IllegalStateException();

      return new BlockPos(destination.getX(), origin.getY() + DRAG_LOWER_BOUND + DRAG_HEIGHT_BOUND, destination.getZ());
   }

   public static boolean isBusyDragging() {
      return origin != null && candidateDestination != null;
   }

   public static boolean isDraggingComplete() {
      return origin != null && finalDestination != null;
   }

   public static BuildingBoundsDragResult getBuildingBoundsDragResult(Level level) {
      if (origin == null || finalDestination == null)
         throw new IllegalStateException();

      Vec3 boxCenterVec3 = AABB.encapsulatingFullBlocks(origin, finalDestination).getCenter();
      BlockPos boxCenter = BlockPos.containing(boxCenterVec3);

      // elevate the center until we find a non-solid block, starting from the lowest point in the dragged box
      BlockPos.MutableBlockPos actualCenter = boxCenter.mutable().setY(origin.getY());
      for (int y = actualCenter.getY(); y < actualCenter.getY() + DRAG_LOWER_BOUND + DRAG_HEIGHT_BOUND; y++) {
         actualCenter.move(Direction.UP);

         BlockState blockState = level.getBlockState(actualCenter);
         if (blockState.isAir()) {
            break;
         }
      }

      BlockPos.MutableBlockPos lowerCorner = origin.mutable();
      BlockPos.MutableBlockPos upperCorner = finalDestination.mutable();

      // swap coordinates to ensure that lower corner's X and Z are always less than upper corner's
      // this is important to ensure that BlockPos traversals inside the bounds using for-loops work intuitively
      if (lowerCorner.getX() > upperCorner.getX()) {
         int oldLX = lowerCorner.getX();
         lowerCorner.setX(upperCorner.getX());
         upperCorner.setX(oldLX);
      }
      if (lowerCorner.getZ() > upperCorner.getZ()) {
         int oldLZ = lowerCorner.getZ();
         lowerCorner.setZ(upperCorner.getZ());
         upperCorner.setZ(oldLZ);
      }
      // don't really need to swap Y, but do it anyway in case things change later
      if (lowerCorner.getY() > upperCorner.getY()) {
         int oldLY = lowerCorner.getY();
         lowerCorner.setY(upperCorner.getY());
         upperCorner.setY(oldLY);
      }

      return new BuildingBoundsDragResult(new BuildingBounds(actualCenter, lowerCorner, upperCorner));
   }

   public record BuildingBoundsDragResult(BuildingBounds bounds) {
   }

   public static void resetDragging() {
      BuildingBoundsDragTool.origin = null;
      BuildingBoundsDragTool.candidateDestination = null;
      BuildingBoundsDragTool.finalDestination = null;
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {

      if (!showDraggedBounds)
         return;

      if (player == null)
         return;

      if (origin == null)
         return;

      if (finalDestination != null) // do not update candidate if final position has been chosen
         return;

      HitResult hitResult = Minecraft.getInstance().hitResult;
      if (!(hitResult instanceof BlockHitResult blockHitResult)) {
         candidateDestination = null;
         return;
      }

      BlockPos lookBlockAir = blockHitResult.getBlockPos().mutable().move(blockHitResult.getDirection());
      candidateDestination = adjustDestination(lookBlockAir);
   }

   @SubscribeEvent
   public static void onRenderLevelStage(RenderLevelStageEvent event) {

      if (!showDraggedBounds)
         return;

      BlockPos destination = finalDestination != null ? finalDestination : candidateDestination;
      if (origin != null && destination != null && event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
         PoseStack poseStack = event.getPoseStack();
         Vec3 camera = event.getCamera().getPosition();
         VertexConsumer consumer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());

         drawRenderBoundingBox(poseStack, consumer, camera, origin, destination);
      }
   }

   @SubscribeEvent
   public static void onPlayerChangeEquipment(PlayerChangedEquipment event) {
      // deeds don't take over while the player is dragging bounds for something else
      if (draggingExternally)
         return;

      Optional<EquipmentChange> equipmentChange = event.getEquipmentChange(EquipmentSlot.MAINHAND);
      if (equipmentChange.isEmpty())
         equipmentChange = event.getEquipmentChange(EquipmentSlot.OFFHAND);
      if (equipmentChange.isEmpty())
         return;

      if (equipmentChange.get().from().getItem() instanceof BuildingDeedItem)
         BuildingBoundsDragTool.saveAndHideDraggedBounds(equipmentChange.get().from());

      if (equipmentChange.get().to().getItem() instanceof BuildingDeedItem)
         BuildingBoundsDragTool.loadAndShowDraggedBounds(equipmentChange.get().to());
   }

   private static void drawRenderBoundingBox(
         PoseStack poseStack,
         VertexConsumer consumer,
         Vec3 camera,
         BlockPos pos1,
         BlockPos pos2) {
      drawBox(poseStack, consumer, camera, pos1, pos2, 0.67F, 0.85F, 0.96F);
   }

   /** Draws the outline of the box between two blocks, e.g. a building's bounds. */
   public static void drawBox(
         PoseStack poseStack,
         VertexConsumer consumer,
         Vec3 camera,
         BlockPos pos1,
         BlockPos pos2,
         float red,
         float green,
         float blue) {
      AABB aabb =
            AABB.encapsulatingFullBlocks(pos1, pos2)
                  .move((double) (-pos1.getX()), (double) (-pos1.getY()), (double) (-pos1.getZ()));
      Vec3 offset = Vec3.atLowerCornerOf(pos1).subtract(camera);
      poseStack.pushPose();
      poseStack.translate(offset.x, offset.y, offset.z);
      ShapeRenderer.renderLineBox(poseStack, consumer, aabb, red, green, blue, 1F);
      poseStack.popPose();
   }
}
