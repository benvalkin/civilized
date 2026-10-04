package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;

import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
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
 * Sent when a player finishes dragging out a new building's bounds. If the building can be placed there, the server
 * either i) displays an 'accepted' chat message to the player {@code confirming} is not yet true, or ii) answers with
 * {@link OpenEstablishBuildingScreen} to show its requirements. Otherwise, it tells the player why the bounds were
 * invalid.
 */
public record RequestEstablishBuilding(BuildingType buildingType, BuildingBounds bounds,
      boolean confirming) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<RequestEstablishBuilding> TYPE =
         new CustomPacketPayload.Type<>(
               ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "request_establish_building"));

   public static final StreamCodec<FriendlyByteBuf, RequestEstablishBuilding> STREAM_CODEC =
         StreamCodec.ofMember(RequestEstablishBuilding::encode, RequestEstablishBuilding::decode);

   public static RequestEstablishBuilding decode(FriendlyByteBuf buffer) {
      return new RequestEstablishBuilding(
            BuildingTypes.getFromResourceLocation(buffer.readResourceLocation()),
            BuildingBounds.decode(buffer),
            buffer.readBoolean());
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeResourceLocation(buildingType.resourceLocation());
      bounds.encode(buffer);
      buffer.writeBoolean(confirming);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveRequestEstablishBuilding(RequestEstablishBuilding packet, IPayloadContext context) {
      ServerPlayer player = (ServerPlayer) context.player();

      PlacementResult placement =
            ServerPlacementChecks.checkEstablish(player, packet.buildingType(), packet.bounds(), packet.confirming());
      switch (placement) {
      case PlacementResult.Failure failure ->
         PacketDistributor.sendToPlayer(player, new BuildingPlacementRejected(failure.reason()));
      case PlacementResult.Confirmed success -> {

         if (!success.confirmed()) {
            player.displayClientMessage(
                  Component.translatable("message.building.placement.help.placed_destination")
                        .withColor(Colors.VALIDATION_PARTIAL_SUCCESS),
                  true);
            return;
         }

         List<RequirementResultData> requirementsResults =
               ServerRequirementChecks
                     .checkEstablish(player, packet.buildingType(), packet.bounds(), success.settlement())
                     .stream()
                     .map(RequirementResultData::from)
                     .toList();
         PacketDistributor.sendToPlayer(
               player,
               new OpenEstablishBuildingScreen(packet.buildingType(), packet.bounds(), requirementsResults));
      }
      }
   }
}
