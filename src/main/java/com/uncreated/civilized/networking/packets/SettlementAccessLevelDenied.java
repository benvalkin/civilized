package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SettlementAccessLevelDenied(Component reason) implements CustomPacketPayload {

   public static final Type<SettlementAccessLevelDenied> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "settlement_access_level_denied"));

   public static final StreamCodec<RegistryFriendlyByteBuf, SettlementAccessLevelDenied> STREAM_CODEC =
         StreamCodec.composite(
               ComponentSerialization.TRUSTED_STREAM_CODEC,
               SettlementAccessLevelDenied::reason,
               SettlementAccessLevelDenied::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
