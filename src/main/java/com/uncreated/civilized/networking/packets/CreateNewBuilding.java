package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.requirement.CurrencyRequirement;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirement;
import com.uncreated.civilized.core.building.requirement.registry.BuildingRequirementList;
import com.uncreated.civilized.core.building.requirement.registry.BuildingRequirements;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.SettlementBounds;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;
import com.uncreated.civilized.core.settlement.permission.AccessLevel;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.util.SettlementUtil;
import com.uncreated.civilized.item.CurrencyItem;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
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

      List<Settlement> thisDimensionSettlements =
            ServerSettlementsStore.INSTANCE.findInDimension(serverLevel.dimension());
      Optional<Settlement> existingSettlement =
            SettlementUtil.findExtendedEncapsulating(
                  thisDimensionSettlements,
                  packet.buildingBounds().getEncapsulatingAABB(),
                  32);

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

            SettlementBounds newBounds = Settlement.calculateBounds(settlementOrigin, List.of(packet.buildingBounds()));
            if (overlapsOtherSettlement(placer, newBounds, thisDimensionSettlements, null))
               return;

            argumentSettlement =
                  ServerSettlementsStore.INSTANCE
                        .createNew(placer.getUUID(), settlementOrigin, serverLevel.dimension());
            ServerSettlementPermissionStore.INSTANCE
                  .setAccessLevel(argumentSettlement.getSettlementId(), placer, AccessLevel.GOVERNOR);
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

               List<BuildingBounds> existingBuildings = new ArrayList<>();
               ServerBuildingsStore.INSTANCE.findForSettlement(argumentSettlement.getSettlementId())
                     .forEach(building -> existingBuildings.add(building.getBounds()));

               existingBuildings.add(packet.buildingBounds());

               SettlementBounds newSettlementBounds = Settlement.calculateBounds(settlementOrigin, existingBuildings);
               if (overlapsOtherSettlement(placer, newSettlementBounds, thisDimensionSettlements, argumentSettlement))
                  return;
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

      debitCurrencyRequirements(packet.buildingType, 1, argumentSettlement, placer);

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

   private static void debitCurrencyRequirements(
         BuildingType buildingType,
         int upgradeLevel,
         Settlement argumentSettlement,
         ServerPlayer placer) {
      BuildingRequirementList requirements = BuildingRequirements.getBuildingRequirements(buildingType, upgradeLevel);
      for (IBuildingRequirement requirement : requirements) {
         if (requirement instanceof CurrencyRequirement c) {
            List<Container> storehouseStorage =
                  ServerBuildingsStore.INSTANCE.findStorehouse(argumentSettlement.getSettlementId())
                        .flatMap(LoadedBuildings::checkLoaded)
                        .map(LoadedBuilding::chests)
                        .orElse(List.of());
            List<Container> townhallStorage =
                  ServerBuildingsStore.INSTANCE.findTownHall(argumentSettlement.getSettlementId())
                        .flatMap(LoadedBuildings::checkLoaded)
                        .map(LoadedBuilding::chests)
                        .orElse(List.of());
            LinkedList<Container> coinStorage = new LinkedList<>();
            // chests are debited in order
            coinStorage.add(placer.getInventory());
            coinStorage.addAll(townhallStorage);
            coinStorage.addAll(storehouseStorage);

            CurrencyItem.debit(coinStorage, c.getRequiredCurrency());
         }
      }
   }

   private static boolean overlapsOtherSettlement(
         ServerPlayer placer,
         SettlementBounds bounds,
         List<Settlement> settlements,
         @Nullable Settlement own) {
      Optional<Settlement> overlapped = Optional.empty();
      for (Settlement other : settlements) {
         if (other != own && other.getBounds().isOverlapping(bounds)
               && (own == null || !other.getBounds().isOverlapping(own.getBounds()))) {
            overlapped = Optional.of(other);
            break;
         }
      }
      if (overlapped.isEmpty())
         return false;

      placer.displayClientMessage(
            Component
                  .translatable(
                        "message.settlement.create_building.failed.too_close_to_other_settlement",
                        overlapped.get().displayNameTranslation())
                  .withColor(Colors.VALIDATION_ERROR),
            true);
      return true;
   }
}
