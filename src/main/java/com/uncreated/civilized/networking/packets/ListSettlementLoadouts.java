package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ListSettlementLoadouts(int requestId, List<Loadout> results) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<ListSettlementLoadouts> TYPE =
         new CustomPacketPayload.Type<>(
               ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "list_settlement_loadouts"));

   public static final StreamCodec<RegistryFriendlyByteBuf, ListSettlementLoadouts> STREAM_CODEC =
         StreamCodec.composite(
               ByteBufCodecs.VAR_INT,
               ListSettlementLoadouts::requestId,
               Loadout.STREAM_CODEC.apply(ByteBufCodecs.list()),
               ListSettlementLoadouts::results,
               ListSettlementLoadouts::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
