package com.uncreated.civilized.core.building.bounds.detection;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PenDetector extends BoundsDetector {

   private static final int PEN_MAX_AREA = 1000;

   @Override
   public BoundsDetectionResult detect(BlockPos start, Level level) {

      Room penSpace = bfsDetectPen(start, level);

      return new BoundsDetectionResult(new LinkedList<>(List.of(penSpace)), penSpace);
   }

   protected Room bfsDetectPen(BlockPos start, Level level) {

      BlockPos first = start.immutable();

      Room penSpace = new Room(first);

      OptionalDouble originBlockHeight = standingHeight(level, first);
      if (originBlockHeight.isEmpty()) {
         penSpace.originObstructed(true);
         return penSpace;
      }

      penSpace.validFloorBlocks().add(first);
      penSpace.passableBlocks().add(first);

      // note: unlike a typical BFS, we use separate visitedFloors and gatePositions sets and only add to them
      // situationally (not immediately after exploring
      // a block). This is because for validFloors, we want to revisit the same neighbor from multiple sides. So we
      // can't immediately mark every neighbor as explored after exploring it, or we'd potentially miss some sides. (A
      // typical visited set does not work for us here)
      // Because we can explore the same neighbor twice, we then also need to avoid adding the same gate twice, hence
      // the use of the gatePositions HashSet.
      Map<BlockPos, Double> visitedFloors = new HashMap<>();
      Set<BlockPos> gatePositions = new HashSet<>();
      visitedFloors.put(first, originBlockHeight.getAsDouble());

      Deque<BlockPos> toSearch = new ArrayDeque<>();
      toSearch.add(first);

      int explored = 0;
      while (!toSearch.isEmpty() && explored < PEN_MAX_AREA) {
         BlockPos pos = toSearch.poll();
         explored++;

         double thisBlockHeight = visitedFloors.get(pos);

         for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int dy = -1; dy <= 1; dy++) {
               BlockPos neighbour = pos.relative(direction).above(dy);

               if (visitedFloors.containsKey(neighbour))
                  continue;

               // an unloaded chunk counts as a leak, without needing to load it
               if (!level.isLoaded(neighbour)) {
                  penSpace.markLeak(neighbour);
                  continue;
               }

               boolean neighborSharesHorizontalFace = dy == 0; // neighbor is right next to the block we are exploring

               OptionalDouble neighborBlockHeight = standingHeight(level, neighbour);
               boolean canJumpFromThisBlockToNeighbor =
                     neighborBlockHeight.isPresent()
                           && neighborBlockHeight.getAsDouble() - thisBlockHeight <= MAX_CLIMB_HEIGHT;
               if (canJumpFromThisBlockToNeighbor) {
                  // if we can jump to this block, it's a valid floor block and part of the pen.

                  visitedFloors.put(neighbour, neighborBlockHeight.getAsDouble());

                  // valid floor blocks do not include water
                  if (level.getFluidState(neighbour).isEmpty())
                     penSpace.validFloorBlocks().add(neighbour);
                  // however they still added as 'passable' blocks in the pen (although this is not particularly useful
                  // information)

                  penSpace.passableBlocks().add(neighbour);
                  toSearch.add(neighbour);
               } else if (neighborSharesHorizontalFace) {
                  // we can't jump to this block - either because the neighbor's standing height is too high (i.e. it's
                  // a gate/door or solid block), or because it is not possible to stand on the neighbor (e.g. an air or
                  // grass block)
                  BlockState state = level.getBlockState(neighbour);
                  if (isDoor(state, false))
                     gatePositions.add(neighbour); // add gate if not exists
                  else if (!isPassable(level, neighbour, state))
                     penSpace.solidBlocks().add(neighbour); // add solid block if not exists
               }
            }
         }
      }

      if (!toSearch.isEmpty())
         penSpace.enclosed(false);

      // we can only detect gates at the end because we need to have a complete "visited" set to know
      // which way doors are facing relative to the current room

      // note: gates on slopes may be missed at the moment. This is fine for now as there are no plans to use
      // the door/gate positions for pen bounds calculations (they are just helpful to have in the result)
      detectDoors(penSpace, gatePositions, visitedFloors.keySet(), level);

      return penSpace;
   }

   private static final double MAX_CLIMB_HEIGHT = 1.25;

   /**
    * The height an animal standing in this block would stand at, or empty if it can't stand here. Animals swim at the
    * surface of water, so they can cross ponds and rivers, and climb back out of them.
    */
   private static OptionalDouble standingHeight(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      BlockPos above = pos.above();
      BlockState aboveState = level.getBlockState(above);
      if (!isPassable(level, pos, state) || !isPassable(level, above, aboveState))
         return OptionalDouble.empty();

      // we treat fluid blocks as blocks that are able to stand on
      FluidState fluidState = level.getFluidState(pos);
      if (fluidState.is(FluidTags.WATER)) {
         // only the top block of water can be "stood on"(or rather "floated in". Lower down, you'd have to swim up.
         if (aboveState.getFluidState().is(FluidTags.WATER))
            return OptionalDouble.empty();
         return OptionalDouble.of(pos.getY() + fluidState.getHeight(level, pos));
      }

      // any other type of fluids (e.g. lava) is not treated as a stand-able block
      if (!fluidState.isEmpty())
         return OptionalDouble.empty();

      // e.g. standing on a carpet or snow layer in this block
      VoxelShape ownShape = state.getCollisionShape(level, pos);
      if (!ownShape.isEmpty())
         return OptionalDouble.of(pos.getY() + ownShape.max(Direction.Axis.Y));

      BlockPos below = pos.below();
      BlockState belowState = level.getBlockState(below);
      if (isPassable(level, below, belowState))
         return OptionalDouble.empty(); // nothing to stand on

      // the top of a fence counts too, at 1.5 above its block. Whether it can be reached is up to MAX_CLIMB_HEIGHT
      return OptionalDouble.of(below.getY() + belowState.getCollisionShape(level, below).max(Direction.Axis.Y));
   }
}
