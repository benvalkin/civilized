package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import com.uncreated.civilized.core.recruitment.VisitorHospitality;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.ui.menu.villager.VillagerMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Hospitality only lives on the server's visitor, so players talking to them are sent a copy whenever it changes.
 */
public record VisitorHospitalityUpdated(int villagerEntityId, VisitorHospitality hospitality)
      implements CustomPacketPayload {

   public static final Type<VisitorHospitalityUpdated> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "visitor_hospitality_updated"));

   public static final StreamCodec<RegistryFriendlyByteBuf, VisitorHospitalityUpdated> STREAM_CODEC =
         StreamCodec.composite(
               ByteBufCodecs.VAR_INT,
               VisitorHospitalityUpdated::villagerEntityId,
               VisitorHospitality.STREAM_CODEC,
               VisitorHospitalityUpdated::hospitality,
               VisitorHospitalityUpdated::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
