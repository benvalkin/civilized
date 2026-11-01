package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.requirement.ServerRequirementContexts;
import com.uncreated.civilized.core.notifications.NotificationService;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.SettlementState;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;
import com.uncreated.civilized.core.settlement.festival.Festival;
import com.uncreated.civilized.core.settlement.festival.FestivalOrganiser;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.item.CurrencyItem;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StartFestival(UUID townSquareId) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<StartFestival> TYPE =
         new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "start_festival"));

   public static final StreamCodec<FriendlyByteBuf, StartFestival> STREAM_CODEC =
         StreamCodec.ofMember(StartFestival::encode, StartFestival::decode);

   public static StartFestival decode(FriendlyByteBuf buffer) {
      return new StartFestival(buffer.readUUID());
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeUUID(townSquareId);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveStartFestival(StartFestival packet, IPayloadContext context) {
      ServerPlayer player = (ServerPlayer) context.player();

      Optional<Building> townSquare = ServerBuildingsStore.INSTANCE.find(packet.townSquareId());
      if (townSquare.isEmpty() || !townSquare.get().getBuildingType().is(BuildingTypes.TOWN_SQUARE))
         return;

      Optional<LoadedSettlement> loaded = LoadedSettlements.checkLoaded(townSquare.get().getSettlementId());
      if (loaded.isEmpty())
         return;

      Settlement settlement = loaded.get().getSettlement();

      if (!ServerSettlementPermissionStore.INSTANCE.getOrCreate(settlement.getSettlementId())
            .hasCreateBuildingsPermission(player.getUUID())) {
         fail(
               player,
               Component.translatable("message.festival.failed.no_permission", settlement.displayNameTranslation()));
         return;
      }

      ServerLevel level = player.server.getLevel(townSquare.get().getDimension());
      if (level == null)
         return;

      long dayTime = level.getDayTime();
      List<Container> coinStorage = ServerRequirementContexts.coinStorage(settlement, player);
      Optional<Component> whyNot =
            Festival.whyCannotStart(settlement.getState(), dayTime, CurrencyItem.countCurrency(coinStorage));
      if (whyNot.isPresent()) {
         fail(player, whyNot.get());
         return;
      }

      CurrencyItem.debit(coinStorage, Festival.cost(settlement.getState()));
      startFestival(settlement, dayTime);

      player.displayClientMessage(
            Component.translatable("message.festival.started").withColor(Colors.VALIDATION_SUCCESS),
            false);
   }

   private static void fail(ServerPlayer player, Component reason) {
      player.displayClientMessage(reason.copy().withColor(Colors.VALIDATION_ERROR), true);
   }

   private static void startFestival(Settlement settlement, long dayTime) {
      SettlementState state = settlement.getState();
      state.setFestivalStartTime(dayTime);
      state.setLastFestivalDay(dayTime / 24000);
      state.setFestivalsHeldCount(state.getFestivalsHeldCount() + 1);
      ServerSettlementsStore.INSTANCE.setDirty();
      ServerSettlementsStore.INSTANCE.replicateChange(settlement, StoreOperation.UPDATE);

      NotificationService.INSTANCE.sendNotification(
            FestivalOrganiser.festivalNotification(
                  settlement,
                  "festival_announced",
                  "notification.festival.announced",
                  "notification.festival.announced.detail"));
   }
}
