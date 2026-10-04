package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Tells the client a building can't be placed where the player dragged the proposed bounds + a reason why. The client
 * shows the reason and clears the dragged bounds.
 */
public record BuildingPlacementRejected(Component reason) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<BuildingPlacementRejected> TYPE =
         new CustomPacketPayload.Type<>(
               ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "building_placement_rejected"));

   public static final StreamCodec<RegistryFriendlyByteBuf, BuildingPlacementRejected> STREAM_CODEC =
         StreamCodec.composite(
               ComponentSerialization.TRUSTED_STREAM_CODEC,
               BuildingPlacementRejected::reason,
               BuildingPlacementRejected::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
