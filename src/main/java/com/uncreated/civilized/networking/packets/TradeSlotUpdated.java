package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TradeSlotUpdated(int sequence, int slot, int newStock, int newAvailableVendorCurrency) implements CustomPacketPayload {

    public static final Type<TradeSlotUpdated> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "trade_slot_updated"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeSlotUpdated> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    TradeSlotUpdated::sequence,
                    ByteBufCodecs.VAR_INT,
                    TradeSlotUpdated::slot,
                    ByteBufCodecs.VAR_INT,
                    TradeSlotUpdated::newStock,
                    ByteBufCodecs.VAR_INT,
                    TradeSlotUpdated::newAvailableVendorCurrency,
                    TradeSlotUpdated::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
