package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.placement.BuildingRedetection;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirementResult;
import com.uncreated.civilized.core.building.requirement.ServerRequirementChecks;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent when a player confirms upgrading a building to its next level, which the server checks again first. */
public record UpgradeBuilding(UUID buildingId) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<UpgradeBuilding> TYPE =
         new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "upgrade_building"));

   public static final StreamCodec<FriendlyByteBuf, UpgradeBuilding> STREAM_CODEC =
         StreamCodec.ofMember(UpgradeBuilding::encode, UpgradeBuilding::decode);

   public static UpgradeBuilding decode(FriendlyByteBuf buffer) {
      return new UpgradeBuilding(buffer.readUUID());
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeUUID(buildingId);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveUpgradeBuilding(UpgradeBuilding packet, IPayloadContext context) {
      ServerPlayer player = (ServerPlayer) context.player();

      Optional<Building> found = ServerBuildingsStore.INSTANCE.find(packet.buildingId());
      if (found.isEmpty())
         return;
      Building building = found.get();

      Optional<Settlement> settlement = ServerSettlementsStore.INSTANCE.find(building.getSettlementId());
      if (settlement.isEmpty())
         return;

      if (!ServerSettlementPermissionStore.INSTANCE.getOrCreate(settlement.get().getSettlementId())
            .hasCreateBuildingsPermission(player.getUUID())) {
         fail(player, "message.building.upgrade.failed.no_permission", settlement.get().displayNameTranslation());
         return;
      }

      ServerLevel level = player.server.getLevel(building.getDimension());
      if (level == null)
         return;

      // re-detect bounds before applying requirements.
      BuildingRedetection.Result redetection = BuildingRedetection.redetect(level, building);
      BuildingBounds bounds;
      if (redetection instanceof BuildingRedetection.Changed(BuildingBounds newBounds))
         bounds = newBounds;
      else
         bounds = building.getBounds();

      Optional<List<IBuildingRequirementResult>> nextLevelRequirements =
            ServerRequirementChecks.checkUpgradeToNextLevel(player, building, bounds);
      if (nextLevelRequirements.isEmpty()) {
         fail(player, "message.building.upgrade.failed.highest_level", building.getBuildingType().translation());
         return;
      }

      // recheck new requirements
      if (!ServerRequirementChecks.allSatisfied(nextLevelRequirements.get())) {
         if (redetection instanceof BuildingRedetection.Failed(com.uncreated.civilized.core.building.placement.PlacementResult.Failure failure))
            player.displayClientMessage(failure.reason(), true);
         else
            fail(
                  player,
                  "message.building.upgrade.failed.requirements_not_met",
                  building.getBuildingType().translation());
         return;
      }

      if (redetection instanceof BuildingRedetection.Changed)
         BuildingRedetection.applyBounds(level, building, bounds);

      int newLevel = building.getUpgradeLevel() + 1;
      ServerRequirementChecks.payCosts(building.getBuildingType(), newLevel, settlement.get(), player);
      building.setUpgradeLevel(newLevel);

      ServerBuildingsStore.INSTANCE.setDirty();
      ServerBuildingsStore.INSTANCE.replicateChange(building, StoreOperation.UPDATE);

      // once the upgrade is finalize, close the menu, otherwise the player can't really see the chat message.
      if (player.containerMenu instanceof BuildingMenu menu
            && menu.getBuilding().getBuildingId().equals(building.getBuildingId()))
         player.closeContainer();

      player.displayClientMessage(
            Component
                  .translatable("message.building.upgrade.success", building.getBuildingType().translation(), newLevel)
                  .withColor(Colors.VALIDATION_SUCCESS),
            false);
   }

   private static void fail(ServerPlayer player, String translationKey, Object... args) {
      player.displayClientMessage(
            Component.translatable(translationKey, args).withColor(Colors.VALIDATION_ERROR),
            true);
   }
}
