package com.uncreated.civilized.core.building.bounds.detection;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.joml.Vector2i;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;

@Getter
@Setter
@Accessors(fluent = true)
public final class Room {
   private final BlockPos origin;
   private boolean enclosed;
   private boolean originObstructed;
   private boolean tooLarge;
   private List<BlockPos> passableBlocks;
   private List<RoomDoor> doors;
   private List<BlockPos> validFloorBlocks;
   private List<BlockPos> solidBlocks;
   private Set<Vector2i> roofedBlocks;

   public Room(BlockPos origin) {
      this.origin = origin;
      this.enclosed = true;
      this.originObstructed = false;
      this.tooLarge = false;
      this.passableBlocks = new LinkedList<>();
      this.doors = new LinkedList<>();
      this.validFloorBlocks = new LinkedList<>();
      this.solidBlocks = new LinkedList<>();
      this.roofedBlocks = new HashSet<>();
   }

   @Override
   public String toString() {
      return String.format(
            "Room[passableBlocks: %s doors: %s enclosed: %s originObstructed: %s, tooLarge: %s]",
            passableBlocks.size(),
            doors.size(),
            enclosed,
            originObstructed,
            tooLarge);
   }
}
