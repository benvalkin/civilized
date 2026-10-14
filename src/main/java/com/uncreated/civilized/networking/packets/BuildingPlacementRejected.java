package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.Optional;

import com.uncreated.civilized.core.building.placement.PlacementResult;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Tells the client a building can't be placed where the player wanted it + a reason why. The client shows the reason
 * and clears the dragged or detected bounds.
 *
 * @param highlight
 *           a block to point out to the player, e.g. the gap in a building's walls
 */
public record BuildingPlacementRejected(Component reason, Optional<BlockPos> highlight) implements CustomPacketPayload {

   public BuildingPlacementRejected(Component reason) {
      this(reason, Optional.empty());
   }

   public BuildingPlacementRejected(PlacementResult.Failure failure) {
      this(failure.reason(), Optional.ofNullable(failure.highlight()));
   }


   public static final CustomPacketPayload.Type<BuildingPlacementRejected> TYPE =
         new CustomPacketPayload.Type<>(
               ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "building_placement_rejected"));

   public static final StreamCodec<RegistryFriendlyByteBuf, BuildingPlacementRejected> STREAM_CODEC =
         StreamCodec.composite(
               ComponentSerialization.TRUSTED_STREAM_CODEC,
               BuildingPlacementRejected::reason,
               ByteBufCodecs.optional(BlockPos.STREAM_CODEC),
               BuildingPlacementRejected::highlight,
               BuildingPlacementRejected::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
