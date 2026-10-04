package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.placement.PlacementResult;
import com.uncreated.civilized.core.building.placement.ServerPlacementChecks;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirementResult;
import com.uncreated.civilized.core.building.requirement.ServerRequirementChecks;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent when a player confirms redrawing a building's bounds. Causes the server to sanity check all the important
 * requirements stuff again.
 */
public record ResizeBuilding(UUID buildingId, BuildingBounds bounds) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<ResizeBuilding> TYPE =
         new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "resize_building"));

   public static final StreamCodec<FriendlyByteBuf, ResizeBuilding> STREAM_CODEC =
         StreamCodec.ofMember(ResizeBuilding::encode, ResizeBuilding::decode);

   public static ResizeBuilding decode(FriendlyByteBuf buffer) {
      return new ResizeBuilding(buffer.readUUID(), BuildingBounds.decode(buffer));
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeUUID(buildingId);
      bounds.encode(buffer);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveResizeBuilding(ResizeBuilding packet, IPayloadContext context) {
      ServerPlayer player = (ServerPlayer) context.player();
      ServerLevel level = player.serverLevel();

      Optional<Building> found = ServerBuildingsStore.INSTANCE.find(packet.buildingId());
      if (found.isEmpty())
         return;
      Building building = found.get();

      // sanity check bounds placement
      PlacementResult placement = ServerPlacementChecks.checkRedraw(player, building, packet.bounds(), true);
      if (placement instanceof PlacementResult.Failure failure) {
         PacketDistributor.sendToPlayer(player, new BuildingPlacementRejected(failure.reason()));
         return;
      }
      Settlement settlement = ((PlacementResult.Confirmed) placement).settlement();

      // sanity check structural requirements again
      List<IBuildingRequirementResult> requirements =
            ServerRequirementChecks.checkRedrawnBounds(player, building, packet.bounds());
      if (!ServerRequirementChecks.allSatisfied(requirements)) {
         player.displayClientMessage(
               Component
                     .translatable(
                           "message.building.redraw.failed.requirements_not_met")
                     .withColor(Colors.VALIDATION_ERROR),
               true);
         return;
      }

      // manually unload the building because it may be part of a new chunk now
      // BAD IMPLEMENTATION: I don't like this at all
      LoadedBuildings.checkLoaded(building).ifPresent(LoadedSettlements::onBuildingUnloaded);
      LoadedBuildings.unload(building.getBuildingId());

      ServerBuildingsStore.INSTANCE.changeBounds(building, packet.bounds());

      if (level.isLoaded(building.getBlockPos())) {
         // re-load the building if the new bounds are in a loaded chunk
         LoadedBuilding loadedBuilding = LoadedBuildings.load(building, level);
         LoadedSettlements.onBuildingLoaded(settlement, loadedBuilding, level);
      }

      // settlement cannot be null when redrawing an existing building's bounds
      assert settlement != null;

      // remember to recalculate the settlement bounds
      settlement.recalculateSettlementBounds(
            settlement.getBounds().getOrigin(),
            ServerBuildingsStore.INSTANCE.findForSettlement(settlement.getSettlementId()));
      ServerSettlementsStore.INSTANCE.setDirty();
      ServerSettlementsStore.INSTANCE.replicateChange(settlement, StoreOperation.ADD_OR_OVERWRITE);

      ServerBuildingsStore.INSTANCE.replicateChange(building, StoreOperation.UPDATE);

      player.displayClientMessage(
            Component.translatable("message.building.redraw.success", building.getBuildingType().translation())
                  .withColor(Colors.VALIDATION_SUCCESS),
            false);
   }
}
