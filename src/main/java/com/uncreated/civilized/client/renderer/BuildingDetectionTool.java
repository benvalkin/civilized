package com.uncreated.civilized.client.renderer;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.time.Duration;
import java.util.List;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.placement.BuildingPlacement;
import com.uncreated.civilized.item.BuildingDeedItem;
import com.uncreated.civilized.networking.packets.BuildingDetected;
import com.uncreated.civilized.networking.packets.RequestEstablishBuilding;
import com.uncreated.civilized.networking.packets.VisualizeBoundsInstruction;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(value = Dist.CLIENT, modid = CIVILIZED_MOD_ID)
public final class BuildingDetectionTool {

   private static final Duration PROBLEM_HIGHLIGHT_DURATION = Duration.ofSeconds(10);
   private static final int PREVIEW_BOUNDS_COLOUR = 0xABD9F5;

   private record Preview(
         BuildingType buildingType,
         BlockPos start,
         BuildingBounds bounds,
         List<VisualizeBoundsInstruction.Marker> markers) {
   }

   private static @Nullable Preview preview;

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

      // air block above clicked pos - this serves as the building's origin
      BlockPos start = clicked.relative(hit.getDirection());
      BuildingBoundsVisualizer.hide(BuildingBoundsVisualizer.Channel.PLACEMENT_PROBLEM);
      PacketDistributor
            .sendToServer(new RequestEstablishBuilding(buildingType, new BuildingPlacement.Detected(start), false));
      return InteractionResult.SUCCESS;
   }

   public static void receiveDetected(BuildingDetected packet) {
      setPreview(new Preview(packet.buildingType(), packet.buildingOrigin(), packet.bounds(), packet.markers()));
      BuildingBoundsVisualizer.hide(BuildingBoundsVisualizer.Channel.PLACEMENT_PROBLEM);

      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null)
         return;

      player.displayClientMessage(
            Component.translatable("message.building.placement.help.detected", packet.buildingType().translation())
                  .withColor(Colors.VALIDATION_PARTIAL_SUCCESS),
            true);
   }

   /**
    * @param highlight
    *           a problematic block to highlight for the player.
    */
   public static void receiveRejected(@Nullable BlockPos highlight) {
      setPreview(null);

      if (highlight == null) {
         BuildingBoundsVisualizer.hide(BuildingBoundsVisualizer.Channel.PLACEMENT_PROBLEM);
         return;
      }

      BuildingBoundsVisualizer.show(
            BuildingBoundsVisualizer.Channel.PLACEMENT_PROBLEM,
            List.of(new VisualizeBoundsInstruction.Marker(highlight, VisualizeBoundsInstruction.MarkerType.LEAK)),
            null,
            0,
            PROBLEM_HIGHLIGHT_DURATION,
            () -> true);
   }

   public static void reset() {
      setPreview(null);
      BuildingBoundsVisualizer.hide(BuildingBoundsVisualizer.Channel.PLACEMENT_PROBLEM);
   }

   private static void setPreview(@Nullable Preview newPreview) {
      preview = newPreview;
      if (newPreview == null) {
         BuildingBoundsVisualizer.hide(BuildingBoundsVisualizer.Channel.PLACEMENT_PREVIEW);
         return;
      }

      BuildingBoundsVisualizer.show(
            BuildingBoundsVisualizer.Channel.PLACEMENT_PREVIEW,
            newPreview.markers(),
            newPreview.bounds(),
            PREVIEW_BOUNDS_COLOUR,
            null,
            () -> isClientPlayerHoldingDeed(newPreview.buildingType()));
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
