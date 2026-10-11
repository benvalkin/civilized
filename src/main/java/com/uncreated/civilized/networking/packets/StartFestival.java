package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.requirement.ServerRequirementContexts;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
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

      Optional<Settlement> settlement = ServerSettlementsStore.INSTANCE.find(townSquare.get().getSettlementId());
      if (settlement.isEmpty())
         return;

      if (!ServerSettlementPermissionStore.INSTANCE.getOrCreate(settlement.get().getSettlementId())
            .hasCreateBuildingsPermission(player.getUUID())) {
         fail(
               player,
               Component.translatable(
                     "message.festival.failed.no_permission",
                     settlement.get().displayNameTranslation()));
         return;
      }

      ServerLevel level = player.server.getLevel(townSquare.get().getDimension());
      if (level == null)
         return;

      long dayTime = level.getDayTime();
      Optional<Component> whyNot = Festival.whyCannotStart(settlement.get().getState(), dayTime);
      if (whyNot.isPresent()) {
         fail(player, whyNot.get());
         return;
      }

      int cost = Festival.cost(settlement.get().getState());
      List<Container> coinStorage = ServerRequirementContexts.coinStorage(settlement.get(), player);
      if (CurrencyItem.countCurrency(coinStorage) < cost) {
         fail(
               player,
               Component.translatable(
                     "message.festival.failed.not_enough_coins",
                     Festival.costTranslation(settlement.get().getState())));
         return;
      }

      CurrencyItem.debit(coinStorage, cost);
      FestivalOrganiser.start(settlement.get(), dayTime);

      player.displayClientMessage(
            Component.translatable("message.festival.started").withColor(Colors.VALIDATION_SUCCESS),
            false);
   }

   private static void fail(ServerPlayer player, Component reason) {
      player.displayClientMessage(reason.copy().withColor(Colors.VALIDATION_ERROR), true);
   }
}
