package com.uncreated.civilized.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class ConnectedBlocks {

   public enum Adjacency {
      /**
       * Blocks that are right next to each other. Does not include diagonals. Up to 6 blocks may be directly next a
       * given block.
       */
      FACES,
      /**
       * Blocks that are right next to or diagonal from each other. Up to 26 (3x3-1) blocks may be directly or
       * diagonally adjacent to a given block.
       */
      ALL
   }

   private ConnectedBlocks() {
   }

   /**
    * @returns A list of blocks similar to the starting block that are connected to it via a vein. The result list
    *          includes the starting block.
    */
   public static List<BlockPos> findSimilarConnectedBlocks(
         BlockGetter level,
         BlockPos start,
         Adjacency adjacency,
         int maxBlocks) {
      Block block = level.getBlockState(start).getBlock();
      return findConnectedBlocks(level, start, adjacency, maxBlocks, (pos, state) -> state.is(block));
   }

   /**
    * @return A list of blocks matching the specified predicate that are connected to the starting block via a vein. The
    *         result list includes the starting block.
    */
   public static List<BlockPos> findConnectedBlocks(
         BlockGetter level,
         BlockPos start,
         Adjacency adjacency,
         int maxBlocks,
         BiPredicate<BlockPos, BlockState> matches) {

      List<BlockPos> found = new ArrayList<>();
      if (!matches.test(start, level.getBlockState(start)))
         return found;

      Set<BlockPos> visited = new HashSet<>();
      Deque<BlockPos> toSearch = new ArrayDeque<>();
      BlockPos first = start.immutable();
      visited.add(first);
      toSearch.add(first);

      while (!toSearch.isEmpty() && found.size() < maxBlocks) {
         BlockPos pos = toSearch.poll();
         found.add(pos);

         for (BlockPos neighbour : neighbours(pos, adjacency)) {
            if (visited.add(neighbour) && matches.test(neighbour, level.getBlockState(neighbour)))
               toSearch.add(neighbour);
         }
      }

      return found;
   }

   private static List<BlockPos> neighbours(BlockPos pos, Adjacency adjacency) {
      List<BlockPos> neighbours = new ArrayList<>(adjacency == Adjacency.ALL ? 26 : 6);

      if (adjacency == Adjacency.FACES) {
         for (Direction direction : Direction.values())
            neighbours.add(pos.relative(direction));
         return neighbours;
      }

      for (int x = -1; x <= 1; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
               if (x != 0 || y != 0 || z != 0)
                  neighbours.add(pos.offset(x, y, z));
            }
         }
      }
      return neighbours;
   }
}
