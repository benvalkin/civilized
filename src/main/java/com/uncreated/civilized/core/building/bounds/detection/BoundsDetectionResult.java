package com.uncreated.civilized.core.building.bounds.detection;

import java.util.Collection;
import java.util.List;

import com.uncreated.civilized.core.building.bounds.BuildingBounds;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;

@Accessors(fluent = true)
@Getter
public class BoundsDetectionResult {
   private final List<Room> rooms;
   private final Room mainRoom;

   boolean anyEnclosed;

   public BoundsDetectionResult(List<Room> rooms, Room mainRoom) {
      this.rooms = rooms;
      this.mainRoom = mainRoom;
   }

   public List<Room> enclosedRooms() {
      return rooms.stream().filter(Room::enclosed).toList();
   }

   public List<Room> unenclosedRooms() {
      return rooms.stream().filter(r -> !r.enclosed()).toList();
   }

   public BuildingBounds createBounds(Collection<Room> rooms) {
      int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
      for (Room room : rooms) {
         for (BlockPos pos : room.passableBlocks()) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
         }
      }

      if (minX == Integer.MAX_VALUE)
         throw new IllegalArgumentException("Cannot create bounds for rooms that have no blocks in them.");

      return new BuildingBounds(
            mainRoom.origin(),
            new BlockPos(minX - 1, minY - 1, minZ - 1),
            new BlockPos(maxX + 1, maxY + 1, maxZ + 1));
   }
}
