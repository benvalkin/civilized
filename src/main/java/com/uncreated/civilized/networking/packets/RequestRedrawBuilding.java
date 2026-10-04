package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
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
 * Like {@link RequestEstablishBuilding}, but for new bounds for an existing building. Valid bounds are answered with
 * {@link OpenRedrawBoundsScreen} once confirmed, which only shows the building's structural requirements.
 *
 * @param confirming
 *           false when the bounds have just been dragged out, true when the player right-clicks inside them
 */
public record RequestRedrawBuilding(UUID buildingId, BuildingBounds bounds, boolean confirming)
      implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<RequestRedrawBuilding> TYPE =
         new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "request_redraw_building"));

   public static final StreamCodec<FriendlyByteBuf, RequestRedrawBuilding> STREAM_CODEC =
         StreamCodec.ofMember(RequestRedrawBuilding::encode, RequestRedrawBuilding::decode);

   public static RequestRedrawBuilding decode(FriendlyByteBuf buffer) {
      return new RequestRedrawBuilding(buffer.readUUID(), BuildingBounds.decode(buffer), buffer.readBoolean());
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeUUID(buildingId);
      bounds.encode(buffer);
      buffer.writeBoolean(confirming);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveRequestRedrawBuilding(RequestRedrawBuilding packet, IPayloadContext context) {
      ServerPlayer player = (ServerPlayer) context.player();

      Optional<Building> building = ServerBuildingsStore.INSTANCE.find(packet.buildingId());
      if (building.isEmpty())
         return;

      PlacementResult placement =
            ServerPlacementChecks.checkRedraw(player, building.get(), packet.bounds(), packet.confirming());
      switch (placement) {
      case PlacementResult.Failure failure ->
         PacketDistributor.sendToPlayer(player, new BuildingPlacementRejected(failure.reason()));
      case PlacementResult.Confirmed confirmed -> {
         if (!confirmed.confirmed()) {
            player.displayClientMessage(
                  Component.translatable("message.building.redraw.help.placed_destination")
                        .withColor(Colors.VALIDATION_PARTIAL_SUCCESS),
                  true);
            return;
         }

         List<RequirementResultData> requirements =
               ServerRequirementChecks.checkRedrawnBounds(player, building.get(), packet.bounds())
                     .stream()
                     .map(RequirementResultData::from)
                     .toList();
         PacketDistributor
               .sendToPlayer(player, new OpenRedrawBoundsScreen(packet.buildingId(), packet.bounds(), requirements));
      }
      }
   }
}
