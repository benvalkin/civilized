package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;

import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Gives the client a preview of an automatically detected building.
 */
public record BuildingDetected(BuildingType buildingType, BlockPos buildingOrigin,
      BuildingBounds bounds, List<VisualizeBoundsInstruction.Marker> markers) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<BuildingDetected> TYPE =
         new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "building_detected"));

   public static final StreamCodec<FriendlyByteBuf, BuildingDetected> STREAM_CODEC =
         StreamCodec.ofMember(BuildingDetected::encode, BuildingDetected::decode);

   public static BuildingDetected decode(FriendlyByteBuf buffer) {
      return new BuildingDetected(
            BuildingTypes.getFromResourceLocation(buffer.readResourceLocation()),
            buffer.readBlockPos(),
            BuildingBounds.decode(buffer),
            buffer.readList(VisualizeBoundsInstruction.Marker.STREAM_CODEC));
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeResourceLocation(buildingType.resourceLocation());
      buffer.writeBlockPos(buildingOrigin);
      bounds.encode(buffer);
      buffer.writeCollection(markers, VisualizeBoundsInstruction.Marker.STREAM_CODEC);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
