package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;

import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.requirement.RequirementResultData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Tells the client to open the screen showing its requirements. If this is sent, the building's dragged bound must have
 * been valid according to the server.
 */
public record OpenEstablishBuildingScreen(BuildingType buildingType, BuildingBounds bounds,
      List<RequirementResultData> requirements) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<OpenEstablishBuildingScreen> TYPE =
         new CustomPacketPayload.Type<>(
               ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "open_establish_building_screen"));

   public static final StreamCodec<RegistryFriendlyByteBuf, OpenEstablishBuildingScreen> STREAM_CODEC =
         StreamCodec.composite(
               ResourceLocation.STREAM_CODEC
                     .map(BuildingTypes::getFromResourceLocation, BuildingType::resourceLocation),
               OpenEstablishBuildingScreen::buildingType,
               StreamCodec.<FriendlyByteBuf, BuildingBounds> of(
                     (buffer, bounds) -> bounds.encode(buffer),
                     BuildingBounds::decode),
               OpenEstablishBuildingScreen::bounds,
               RequirementResultData.STREAM_CODEC.apply(ByteBufCodecs.list()),
               OpenEstablishBuildingScreen::requirements,
               OpenEstablishBuildingScreen::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
