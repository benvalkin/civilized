package com.uncreated.civilized.core.building.bounds.detection;

import java.util.List;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class BoundsDetectionResult {
   private final List<Room> rooms;
   private final Room mainRoom;

   boolean anyEnclosed;

   public BoundsDetectionResult(List<Room> rooms, Room mainRoom) {
      this.rooms = rooms.stream().filter(r -> !r.tooLarge()).toList();
      this.mainRoom = mainRoom;
   }

   public List<Room> enclosedRooms() {
      return rooms.stream().filter(Room::enclosed).toList();
   }

   public List<Room> unenclosedRooms() {
      return rooms.stream().filter(r -> !r.enclosed()).toList();
   }
}
