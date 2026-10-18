package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record ListSoldierEquipment(int requestId, Optional<List<ItemStack>> items) implements CustomPacketPayload {

   public static final Type<ListSoldierEquipment> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "list_soldier_equipment"));

   public static final StreamCodec<RegistryFriendlyByteBuf, ListSoldierEquipment> STREAM_CODEC =
         StreamCodec.composite(
               ByteBufCodecs.VAR_INT,
               ListSoldierEquipment::requestId,
               ByteBufCodecs.optional(ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list())),
               ListSoldierEquipment::items,
               ListSoldierEquipment::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
