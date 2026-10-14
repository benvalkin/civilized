package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.bounds.BuildingBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TellClientVisualizeBounds(List<Marker> markers,
                                        Optional<BuildingBounds> bounds) implements CustomPacketPayload {

   public static final TellClientVisualizeBounds CLEAR = new TellClientVisualizeBounds(List.of(), Optional.empty());

   public enum MarkerType {
      ENCLOSED_FLOOR, UNENCLOSED_FLOOR, DOOR, BOUNDARY, START, LEAK
   }

   public record Marker(BlockPos pos, MarkerType type) {
   }

   public static final CustomPacketPayload.Type<TellClientVisualizeBounds> TYPE =
         new CustomPacketPayload.Type<>(
               ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "show_bounds_detection"));

   public static final StreamCodec<FriendlyByteBuf, TellClientVisualizeBounds> STREAM_CODEC =
         StreamCodec.ofMember(TellClientVisualizeBounds::encode, TellClientVisualizeBounds::decode);

   public boolean isClear() {
      return markers.isEmpty() && bounds.isEmpty();
   }

   public static TellClientVisualizeBounds decode(FriendlyByteBuf buffer) {
      List<Marker> markers = buffer.readList(b -> new Marker(BlockPos.of(b.readLong()), b.readEnum(MarkerType.class)));
      Optional<BuildingBounds> bounds = buffer.readOptional(BuildingBounds::decode);
      return new TellClientVisualizeBounds(markers, bounds);
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeCollection(markers, (b, marker) -> {
         b.writeLong(marker.pos().asLong());
         b.writeEnum(marker.type());
      });
      buffer.writeOptional(bounds, (b, value) -> value.encode(b));
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
