package com.uncreated.civilized.core.building.bounds.detection;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.joml.Vector2i;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

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
   /** The first block that leads outside if the room is not enclosed. */
   @Setter(AccessLevel.NONE)
   private @Nullable BlockPos leak;

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

   public void markLeak(BlockPos outsidePos) {
      enclosed = false;
      if (leak == null)
         leak = outsidePos.immutable();
   }

   void detectFloorBlocks(Level level) {
      Set<BlockPos> passable = new HashSet<>(passableBlocks);
      for (BlockPos pos : passableBlocks) {
         boolean somethingToStandOn = !passable.contains(pos.below());
         boolean headroom = passable.contains(pos.above());
         if (somethingToStandOn && headroom && level.getFluidState(pos).isEmpty())
            validFloorBlocks.add(pos);
      }
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
