package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.core.settlement.loadouts.ServerLoadoutsStore;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LoadoutSavedResult(Loadout loadout, boolean success) implements CustomPacketPayload {

   public static final Type<LoadoutSavedResult> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "loadout_saved_result"));

   public static final StreamCodec<RegistryFriendlyByteBuf, LoadoutSavedResult> STREAM_CODEC =
         StreamCodec.ofMember(LoadoutSavedResult::encode, LoadoutSavedResult::decode);

   public static LoadoutSavedResult decode(RegistryFriendlyByteBuf buffer) {
      return new LoadoutSavedResult(Loadout.STREAM_CODEC.decode(buffer), buffer.readBoolean());
   }

   public void encode(RegistryFriendlyByteBuf buffer) {
      Loadout.STREAM_CODEC.encode(buffer, loadout);
      buffer.writeBoolean(success);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
