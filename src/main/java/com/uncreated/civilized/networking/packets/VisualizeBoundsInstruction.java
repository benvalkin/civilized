package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;
import static com.uncreated.civilized.core.building.bounds.detection.BuildingDetectionType.PEN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.bounds.detection.BuildingDetectionType;
import com.uncreated.civilized.core.building.bounds.detection.Room;
import com.uncreated.civilized.core.building.bounds.detection.RoomDoor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VisualizeBoundsInstruction(List<Marker> markers,
      Optional<BuildingBounds> bounds) implements CustomPacketPayload {

   /** Keeps huge rooms from sending the player an unnecessary number of markers. */
   private static final int MAX_MARKERS = 1000;

   public static final VisualizeBoundsInstruction CLEAR = new VisualizeBoundsInstruction(List.of(), Optional.empty());

   public enum MarkerType {
      ENCLOSED_FLOOR, UNENCLOSED_FLOOR, DOOR, BOUNDARY, START, LEAK
   }

   public record Marker(BlockPos pos, MarkerType type) {

      public static final StreamCodec<FriendlyByteBuf, Marker> STREAM_CODEC = StreamCodec.of((buffer, marker) -> {
         buffer.writeLong(marker.pos().asLong());
         buffer.writeEnum(marker.type());
      }, buffer -> new Marker(BlockPos.of(buffer.readLong()), buffer.readEnum(MarkerType.class)));
   }


   public static List<Marker> allMarkerTypes(List<Room> rooms, BuildingDetectionType type) {
      List<Marker> markers = new ArrayList<>(VisualizeBoundsInstruction.floorAndDoorMarkers(rooms));
      for (Room room : rooms) {
         // a pen's fences and the terrain walling it in, which is what decides where it ends
         if (type == PEN)
            for (BlockPos pos : room.solidBlocks())
               markers.add(new Marker(pos, MarkerType.BOUNDARY));

         markers.add(new Marker(room.origin(), MarkerType.START));
         if (room.leak() != null)
            markers.add(new Marker(room.leak(), MarkerType.LEAK));
      }

      return markers.size() > MAX_MARKERS ? markers.subList(0, MAX_MARKERS) : markers;
   }

   public static List<Marker> floorAndDoorMarkers(Collection<Room> rooms) {
      List<Marker> markers = new ArrayList<>();
      for (Room room : rooms) {
         MarkerType floor = room.enclosed() ? MarkerType.ENCLOSED_FLOOR : MarkerType.UNENCLOSED_FLOOR;
         for (BlockPos pos : room.validFloorBlocks())
            markers.add(new Marker(pos, floor));

         for (RoomDoor door : room.doors())
            markers.add(new Marker(door.doorOrigin(), MarkerType.DOOR));
      }
      return markers;
   }

   public static final CustomPacketPayload.Type<VisualizeBoundsInstruction> TYPE =
         new CustomPacketPayload.Type<>(
               ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "show_bounds_detection"));

   public static final StreamCodec<FriendlyByteBuf, VisualizeBoundsInstruction> STREAM_CODEC =
         StreamCodec.ofMember(VisualizeBoundsInstruction::encode, VisualizeBoundsInstruction::decode);

   public boolean isClear() {
      return markers.isEmpty() && bounds.isEmpty();
   }

   public static VisualizeBoundsInstruction decode(FriendlyByteBuf buffer) {
      List<Marker> markers = buffer.readList(Marker.STREAM_CODEC);
      Optional<BuildingBounds> bounds = buffer.readOptional(BuildingBounds::decode);
      return new VisualizeBoundsInstruction(markers, bounds);
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeCollection(markers, Marker.STREAM_CODEC);
      buffer.writeOptional(bounds, (b, value) -> value.encode(b));
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
