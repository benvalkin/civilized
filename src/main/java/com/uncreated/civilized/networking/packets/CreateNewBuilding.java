package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
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
import com.uncreated.civilized.core.building.placement.PlacementResult;
import com.uncreated.civilized.core.building.placement.ServerPlacementChecks;
import com.uncreated.civilized.core.building.requirement.CurrencyRequirement;
import com.uncreated.civilized.core.building.requirement.ServerRequirementContexts;
import com.uncreated.civilized.core.building.requirement.ServerRequirementChecks;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirementResult;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirement;
import com.uncreated.civilized.core.building.requirement.registry.BuildingRequirementList;
import com.uncreated.civilized.core.building.requirement.registry.BuildingRequirements;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;
import com.uncreated.civilized.core.settlement.permission.AccessLevel;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
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

      // sanity check the boudns placement again
      PlacementResult placement =
            ServerPlacementChecks.checkEstablish(placer, packet.buildingType(), packet.buildingBounds());
      if (placement instanceof PlacementResult.Failure failure) {
         placer.displayClientMessage(failure.reason(), true);
         return;
      }
      @Nullable
      Settlement nearbySettlement = ((PlacementResult.Success) placement).settlement();

      List<IBuildingRequirementResult> requirements =
            ServerRequirementChecks
                  .checkEstablish(placer, packet.buildingType(), packet.buildingBounds(), nearbySettlement);
      if (!ServerRequirementChecks.allSatisfied(requirements)) {
         placer.displayClientMessage(
               Component
                     .translatable(
                           "message.settlement.create_building.failed.requirements_not_met",
                           packet.buildingType().translation())
                     .withColor(Colors.VALIDATION_ERROR),
               true);
         return;
      }

      // nothing is created until everything has been checked
      Settlement argumentSettlement;
      BlockPos settlementOrigin;
      if (nearbySettlement == null) {
         settlementOrigin = packet.buildingBounds().getCenter();
         argumentSettlement =
               ServerSettlementsStore.INSTANCE.createNew(placer.getUUID(), settlementOrigin, serverLevel.dimension());
         ServerSettlementPermissionStore.INSTANCE
               .setAccessLevel(argumentSettlement.getSettlementId(), placer, AccessLevel.GOVERNOR);
      } else {
         argumentSettlement = nearbySettlement;
         settlementOrigin = argumentSettlement.getBounds().getOrigin();
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

      debitCurrencyRequirements(packet.buildingType, Building.FIRST_UPGRADE_LEVEL, argumentSettlement, placer);
      placer.getMainHandItem().consume(1, placer);

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
            List<Container> coinStorage = ServerRequirementContexts.coinStorage(argumentSettlement, placer);
            CurrencyItem.debit(coinStorage, c.getRequiredCurrency());
         }
      }
   }
}
