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

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent when a player finishes dragging out a new building's bounds. If the building can be placed there, the server
 * answers with {@link OpenEstablishBuildingScreen} to show its requirements. Otherwise, it tells the player why not.
 */
public record RequestEstablishBuilding(BuildingType buildingType, BuildingBounds bounds) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<RequestEstablishBuilding> TYPE =
         new CustomPacketPayload.Type<>(
               ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "request_establish_building"));

   public static final StreamCodec<FriendlyByteBuf, RequestEstablishBuilding> STREAM_CODEC =
         StreamCodec.ofMember(RequestEstablishBuilding::encode, RequestEstablishBuilding::decode);

   public static RequestEstablishBuilding decode(FriendlyByteBuf buffer) {
      return new RequestEstablishBuilding(
            BuildingTypes.getFromResourceLocation(buffer.readResourceLocation()),
            BuildingBounds.decode(buffer));
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeResourceLocation(buildingType.resourceLocation());
      bounds.encode(buffer);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveRequestEstablishBuilding(RequestEstablishBuilding packet, IPayloadContext context) {
      ServerPlayer player = (ServerPlayer) context.player();

      PlacementResult placement = ServerPlacementChecks.checkEstablish(player, packet.buildingType(), packet.bounds());
      switch (placement) {
      case PlacementResult.Failure failure -> player.displayClientMessage(failure.reason(), true);
      case PlacementResult.Success success -> {
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
