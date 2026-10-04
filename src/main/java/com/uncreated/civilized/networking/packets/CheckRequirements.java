package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirementResult;
import com.uncreated.civilized.core.building.requirement.RequirementCheckPurpose;
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
 * Asks the server to check a building's requirements, which it answers with {@link RequirementsChecked}.
 *
 * @param requestId
 *           sent back with the answer, so that screens can make sure they receive the right answer and ignore stale
 *           ones.
 */
public record CheckRequirements(int requestId, RequirementCheckPurpose purpose, @Nullable BuildingType buildingType,
      @Nullable BuildingBounds bounds, @Nullable UUID buildingId) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<CheckRequirements> TYPE =
         new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "check_requirements"));

   public static final StreamCodec<FriendlyByteBuf, CheckRequirements> STREAM_CODEC =
         StreamCodec.ofMember(CheckRequirements::encode, CheckRequirements::decode);

   private static int lastRequestId = 0;

   public static int nextRequestId() {
      return ++lastRequestId;
   }

   public static CheckRequirements forEstablish(int requestId, BuildingType buildingType, BuildingBounds bounds) {
      return new CheckRequirements(requestId, RequirementCheckPurpose.ESTABLISH, buildingType, bounds, null);
   }

   public static CheckRequirements forUpgrade(int requestId, UUID buildingId) {
      return new CheckRequirements(requestId, RequirementCheckPurpose.UPGRADE, null, null, buildingId);
   }

   public static CheckRequirements forRedraw(int requestId, UUID buildingId, BuildingBounds newBounds) {
      return new CheckRequirements(requestId, RequirementCheckPurpose.REDRAW, null, newBounds, buildingId);
   }

   public static CheckRequirements decode(FriendlyByteBuf buffer) {
      return new CheckRequirements(
            buffer.readVarInt(),
            buffer.readEnum(RequirementCheckPurpose.class),
            buffer.readNullable(b -> BuildingTypes.getFromResourceLocation(b.readResourceLocation())),
            buffer.readNullable(BuildingBounds::decode),
            buffer.readNullable(b -> b.readUUID()));
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeVarInt(requestId);
      buffer.writeEnum(purpose);
      buffer.writeNullable(buildingType, (b, type) -> b.writeResourceLocation(type.resourceLocation()));
      buffer.writeNullable(bounds, (b, value) -> value.encode(b));
      buffer.writeNullable(buildingId, (b, id) -> b.writeUUID(id));
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveCheckRequirements(CheckRequirements packet, IPayloadContext context) {
      ServerPlayer player = (ServerPlayer) context.player();

      Optional<List<IBuildingRequirementResult>> results = packet.check(player);
      RequirementsChecked result;
      if (results.isEmpty())
         // malformed requests and upgrades for buildings already at their highest level are answered with no results
         result = new RequirementsChecked(packet.requestId(), List.of());
      else {
         List<RequirementResultData> requirementResultData =
               results.get().stream().map(RequirementResultData::from).toList();
         result = new RequirementsChecked(packet.requestId(), requirementResultData);
      }

      PacketDistributor.sendToPlayer(player, result);
   }

   private Optional<List<IBuildingRequirementResult>> check(ServerPlayer player) {
      switch (purpose) {
      case ESTABLISH -> {
         if (buildingType == null || bounds == null)
            return Optional.empty();

         return Optional.of(
               ServerRequirementChecks.checkEstablish(
                     player,
                     buildingType,
                     bounds,
                     ServerRequirementChecks.findSettlementForNew(player.serverLevel(), buildingType, bounds)));
      }
      case UPGRADE -> {
         return findBuilding().flatMap(building -> ServerRequirementChecks.checkUpgradeToNextLevel(player, building));
      }
      case REDRAW -> {
         if (bounds == null)
            return Optional.empty();

         return findBuilding().map(building -> ServerRequirementChecks.checkRedrawnBounds(player, building, bounds));
      }
      }
      return Optional.empty();
   }

   private Optional<Building> findBuilding() {
      return ServerBuildingsStore.INSTANCE.find(buildingId);
   }
}
