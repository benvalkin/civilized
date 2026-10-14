package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;

import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.placement.BuildingPlacement;
import com.uncreated.civilized.core.building.placement.PlacementResult;
import com.uncreated.civilized.core.building.placement.ServerPlacementChecks;
import com.uncreated.civilized.core.building.requirement.RequirementResultData;
import com.uncreated.civilized.core.building.requirement.ServerRequirementChecks;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent when a player finishes dragging out a new building's bounds, or clicks inside it to detect its rooms. If the
 * building can be placed there, the server either i) tells the player it's been accepted if {@code confirming} is not
 * yet true or ii) answers with {@link OpenEstablishBuildingScreen} to show its requirements. Otherwise, it tells the
 * player why the placement was invalid.
 */
public record RequestEstablishBuilding(BuildingType buildingType, BuildingPlacement placement,
      boolean confirming) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<RequestEstablishBuilding> TYPE =
         new CustomPacketPayload.Type<>(
               ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "request_establish_building"));

   public static final StreamCodec<FriendlyByteBuf, RequestEstablishBuilding> STREAM_CODEC =
         StreamCodec.ofMember(RequestEstablishBuilding::encode, RequestEstablishBuilding::decode);

   public static RequestEstablishBuilding decode(FriendlyByteBuf buffer) {
      return new RequestEstablishBuilding(
            BuildingTypes.getFromResourceLocation(buffer.readResourceLocation()),
            BuildingPlacement.STREAM_CODEC.decode(buffer),
            buffer.readBoolean());
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeResourceLocation(buildingType.resourceLocation());
      BuildingPlacement.STREAM_CODEC.encode(buffer, placement);
      buffer.writeBoolean(confirming);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveRequestEstablishBuilding(RequestEstablishBuilding packet, IPayloadContext context) {
      ServerPlayer player = (ServerPlayer) context.player();

      PlacementResult placement =
            ServerPlacementChecks
                  .checkEstablish(player, packet.buildingType(), packet.placement(), packet.confirming());
      switch (placement) {
      case PlacementResult.Failure failure ->
         PacketDistributor.sendToPlayer(player, new BuildingPlacementRejected(failure));
      case PlacementResult.Confirmed success -> {

         if (!success.confirmed()) {
            if (packet.placement() instanceof BuildingPlacement.Detected(net.minecraft.core.BlockPos start))
               PacketDistributor
                     .sendToPlayer(player, new BuildingDetected(packet.buildingType(), start, success.bounds()));
            else
               player.displayClientMessage(
                     Component.translatable("message.building.placement.help.placed_destination")
                           .withColor(Colors.VALIDATION_PARTIAL_SUCCESS),
                     true);
            return;
         }

         List<RequirementResultData> requirementsResults =
               ServerRequirementChecks
                     .checkEstablish(player, packet.buildingType(), success.bounds(), success.settlement())
                     .stream()
                     .map(RequirementResultData::from)
                     .toList();
         PacketDistributor.sendToPlayer(
               player,
               new OpenEstablishBuildingScreen(packet.buildingType(), packet.placement(), requirementsResults));
      }
      }
   }
}
