package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;

import com.uncreated.civilized.core.building.requirement.RequirementResultData;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The server's answer to {@link CheckRequirements}. */
public record RequirementsChecked(int requestId, List<RequirementResultData> results) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<RequirementsChecked> TYPE =
         new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "requirements_checked"));

   public static final StreamCodec<RegistryFriendlyByteBuf, RequirementsChecked> STREAM_CODEC =
         StreamCodec.composite(
               ByteBufCodecs.VAR_INT,
               RequirementsChecked::requestId,
               RequirementResultData.STREAM_CODEC.apply(ByteBufCodecs.list()),
               RequirementsChecked::results,
               RequirementsChecked::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
