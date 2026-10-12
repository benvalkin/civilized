package com.uncreated.civilized.core.building.bounds;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.joml.Vector2i;

import com.uncreated.civilized.util.ConnectedBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BoundsDetector {

   public List<Room> detect(BlockPos start, Level level) {

      BlockPos first = start.immutable();

      Deque<Room> rooms = new LinkedList<>();
      Room mainRoom = new Room(first);
      rooms.add(mainRoom);

      Set<BlockPos> visited = new HashSet<>();
      List<Room> finalRooms = new ArrayList<>();

      while (!rooms.isEmpty()) {
         Room room = rooms.poll();

         floodFillDetectRoomFeatures(level, room, visited);

         finalRooms.add(room);

         if (!room.enclosed())
            continue; // don't bother trying to explore adjacent doors for a room that's unenclosed

         for (RoomDoor door : room.doors()) {
            BlockPos newRoomOrigin = door.outside();
            Room newRoom = new Room(newRoomOrigin);
            rooms.add(newRoom);
         }
      }

      return finalRooms;
   }

   private static final double MAX_STEP_OVER_HEIGHT = 0.25;

   private static boolean isOutside(LevelReader level, BlockPos pos) {
      return pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
   }

   private static boolean isPassable(BlockGetter level, BlockPos pos, BlockState state) {
      if (state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS) || state.is(BlockTags.FENCE_GATES))
         return false; // doors count as parts of walls.

      if (state.is(BlockTags.CLIMBABLE))
         return true; // ladders and vines can be passed through

      VoxelShape collisionShape = state.getCollisionShape(level, pos);
      if (collisionShape.isEmpty()) // block has no collision
         return true;

      // only very low blocks (e.g. carpets and snow layers) will pass this check. Anything taller, like slabs, stairs,
      // beds and chests, is part of the floor or furniture. Thin but tall blocks (e.g. glass panes, iron bars, fences)
      // are part of the walls
      return collisionShape.max(Direction.Axis.Y) <= MAX_STEP_OVER_HEIGHT;
   }

   private static boolean isDoor(BlockState state) {
      return state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS) || state.is(BlockTags.FENCE_GATES);
   }

   private static boolean isDoorOrigin(BlockState state) {
      return isDoor(state) && (!state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
            || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER);
   }

   private static final int ROOM_MAX_VOLUME = 5000;

   private static void floodFillDetectRoomFeatures(LevelReader level, Room room, Set<BlockPos> visited) {

      BlockPos start = room.origin();

      if (!isPassable(level, start, level.getBlockState(start))) {
         room.originObstructed(true);
         return;
      }

      Deque<BlockPos> toSearch = new ArrayDeque<>();
      BlockPos first = start.immutable();
      visited.add(first);
      toSearch.add(first);

      List<BlockPos> doorPositions = new ArrayList<>();

      int explored = 0;
      while (!toSearch.isEmpty() && explored < ROOM_MAX_VOLUME) {
         BlockPos pos = toSearch.poll();
         explored++;

         for (BlockPos neighbour : ConnectedBlocks.neighbours(pos, ConnectedBlocks.Adjacency.FACES)) {
            // search Adjacency.FACES to avoid searching through diagonal walls
            boolean alreadyVisited = !visited.add(neighbour);
            if (alreadyVisited)
               continue;

            // edge case handling for if this chunk is not loaded: make sure to call isOutside (which does not load
            // unloaded chunks) before getBlockState (which loads unloaded chunks) so that unloaded chunks are treated
            // as "outside". Do we really need to do this?
            if (isOutside(level, neighbour)) {
               if (isPassable(level, neighbour, level.getBlockState(neighbour)))
                  room.enclosed(false);
               continue;
            }

            BlockState blockState = level.getBlockState(neighbour);

            if (isPassable(level, neighbour, blockState)) {
               room.passableBlocks().add(neighbour);
               room.roofedBlocks().add(new Vector2i(neighbour.getX(), neighbour.getZ()));
               toSearch.add(neighbour);
            } else {
               if (!isDoor(blockState))
                  room.solidBlocks().add(neighbour);
               else if (isDoorOrigin(blockState))
                  doorPositions.add(neighbour);
            }
         }
      }

      if (explored >= ROOM_MAX_VOLUME) {
         room.enclosed(false);
      }

      // we can only populate RoomDoors do this at the end because we need to have a complete "visited" set to know
      // which way doors are facing relative to the current room
      for (BlockPos doorPosition : doorPositions) {
         BlockState doorState = level.getBlockState(doorPosition);
         Optional<RoomDoor> roomDoor = RoomDoor.fromDoorPos(doorPosition, doorState, visited);

         // don't add the door if it leads somewhere we've already seen - this means the door doesn't actually go
         // anywhere
         if (roomDoor.isEmpty() || visited.contains(roomDoor.get().outside()))
            continue;

         room.doors().add(roomDoor.get());
      }
   }
}
