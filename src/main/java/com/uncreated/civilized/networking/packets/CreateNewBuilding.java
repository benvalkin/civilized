package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.Optional;
import java.util.Set;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;
import com.uncreated.civilized.core.settlement.permission.AccessLevel;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CreateNewBuilding(BuildingType buildingType,
      BuildingBounds buildingBounds) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<CreateNewBuilding> TYPE =
         new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "create_new_building"));

   public static StreamCodec<FriendlyByteBuf, CreateNewBuilding> STREAM_CODEC =
         StreamCodec.ofMember(CreateNewBuilding::encode, CreateNewBuilding::decode);

   public static CreateNewBuilding decode(FriendlyByteBuf buffer) {
      return new CreateNewBuilding(
            BuildingTypes.getFromResourceLocation(buffer.readResourceLocation()),
            BuildingBounds.decode(buffer));
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeResourceLocation(buildingType.resourceLocation());
      buildingBounds.encode(buffer);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveCreateNewBuilding(CreateNewBuilding packet, IPayloadContext context) {

      ServerPlayer placer = (ServerPlayer) context.player();
      ServerLevel serverLevel = placer.serverLevel();

      Optional<Settlement> existingSettlement =
            ServerSettlementsStore.INSTANCE.all()
                  .stream()
                  .filter(
                        s -> s.getBounds()
                              .getEncapsulatingAABB()
                              .intersects(packet.buildingBounds().getEncapsulatingAABB()))
                  .findFirst();

      Settlement argumentSettlement;
      BlockPos settlementOrigin;
      if (packet.buildingType().is(BuildingTypes.TOWN_HALL)) {
         if (existingSettlement.isPresent()) {
            placer.displayClientMessage(
                  Component
                        .translatable(
                              "message.settlement.create_building.failed.building_already_exists",
                              existingSettlement.get().displayNameTranslation(),
                              packet.buildingType().translation())
                        .withColor(Colors.VALIDATION_ERROR),
                  true);
            return;
         } else {
            if (ServerSettlementsStore.INSTANCE.findFromOwner(placer.getUUID()).isPresent()) {
               placer.displayClientMessage(
                     Component
                           .translatable(
                                 "message.settlement.create_building.failed.player_already_has_another_settlement")
                           .withColor(Colors.VALIDATION_ERROR),
                     true);
               return;
            }

            settlementOrigin = packet.buildingBounds.getCenter();
            argumentSettlement = ServerSettlementsStore.INSTANCE.createNew(placer.getUUID(), settlementOrigin);
            SettlementPermissions permissions =
                  ServerSettlementPermissionStore.INSTANCE.getOrCreate(argumentSettlement.getSettlementId());
            permissions.setAccessLevel(placer, AccessLevel.GOVERNOR);
         }
      } else {
         if (existingSettlement.isPresent()) {
            if (!ServerSettlementPermissionStore.INSTANCE.getOrCreate(existingSettlement.get().getSettlementId())
                  .hasCreateBuildingsPermission(placer.getUUID())) {
               placer.displayClientMessage(
                     Component
                           .translatable(
                                 "message.settlement.create_building.failed.no_permission",
                                 existingSettlement.get().displayNameTranslation())
                           .withColor(Colors.VALIDATION_ERROR),
                     true);
               return;
            } else {
               argumentSettlement = existingSettlement.get();
               settlementOrigin = argumentSettlement.getBounds().getOrigin();
            }
         } else {
            placer.displayClientMessage(
                  Component.translatable("message.settlement.create_building.failed.too_far_from_settlement")
                        .withColor(Colors.VALIDATION_ERROR),
                  true);
            return;
         }
      }

      Building building =
            ServerBuildingsStore.INSTANCE.createNew(
                  context.player().registryAccess(),
                  serverLevel.dimension(),
                  argumentSettlement.getSettlementId(),
                  placer.getUUID(),
                  packet.buildingType(),
                  packet.buildingBounds());

      if (serverLevel.isLoaded(building.getBlockPos())) {
         LoadedBuilding loadedBuilding = LoadedBuildings.load(building, serverLevel);
         LoadedSettlements.onBuildingLoaded(argumentSettlement, loadedBuilding, serverLevel);
      }

      Set<Building> settlementBuildings =
            ServerBuildingsStore.INSTANCE.findForSettlement(argumentSettlement.getSettlementId());
      argumentSettlement.recalculateSettlementBounds(settlementOrigin, settlementBuildings);

      ServerSettlementsStore.INSTANCE.setDirty();
      ServerSettlementsStore.INSTANCE.replicateChange(argumentSettlement, StoreOperation.ADD_OR_OVERWRITE);

      ServerBuildingsStore.INSTANCE.setDirty();
      ServerBuildingsStore.INSTANCE.replicateChange(building, StoreOperation.ADD_OR_OVERWRITE);

      placer.displayClientMessage(
            Component
                  .translatable(
                        "message.building.placement.validation.success",
                        building.getBuildingType().translation())
                  .withColor(Colors.VALIDATION_SUCCESS),
            false);
   }
}
