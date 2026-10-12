package com.uncreated.civilized.core.building.bounds;

import java.util.ArrayList;
import java.util.HashSet;
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
   private List<BlockPos> passableBlocks;
   private List<RoomDoor> doors;
   private List<BlockPos> validFloorBlocks;
   private List<BlockPos> solidBlocks;
   private Set<Vector2i> roofedBlocks;

   public Room(
         BlockPos origin,
         List<BlockPos> passableBlocks,
         List<BlockPos> validFloorBlocks,
         List<BlockPos> solidBlocks,
         Set<Vector2i> roofedBlocks) {
      this.origin = origin;
      this.enclosed = true;
      this.originObstructed = false;
      this.passableBlocks = passableBlocks;
      this.validFloorBlocks = validFloorBlocks;
      this.solidBlocks = solidBlocks;
      this.roofedBlocks = roofedBlocks;
   }

   public Room(BlockPos origin) {
      this(origin, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new HashSet<>());
   }

   @Override
   public String toString() {
      return String.format("Room[passableBlocks: %s", passableBlocks);
   }
}
