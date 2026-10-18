package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.core.settlement.loadouts.ServerLoadoutsStore;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestSettlementLoadouts(int requestId, UUID settlementId) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<RequestSettlementLoadouts> TYPE =
         new CustomPacketPayload.Type<>(
               ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "request_settlement_loadouts"));

   public static final StreamCodec<FriendlyByteBuf, RequestSettlementLoadouts> STREAM_CODEC =
         StreamCodec.ofMember(RequestSettlementLoadouts::encode, RequestSettlementLoadouts::decode);

   private static int lastRequestId = 0;

   public static int nextRequestId() {
      return ++lastRequestId;
   }

   public static RequestSettlementLoadouts decode(FriendlyByteBuf buffer) {
      return new RequestSettlementLoadouts(buffer.readVarInt(), buffer.readUUID());
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeVarInt(requestId);
      buffer.writeUUID(settlementId);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveRequest(RequestSettlementLoadouts packet, IPayloadContext context) {

      List<Loadout> loadouts = new LinkedList<>(ServerLoadoutsStore.INSTANCE.findForSettlement(packet.settlementId()));

      context.reply(new ListSettlementLoadouts(packet.requestId(), loadouts));
   }
}
