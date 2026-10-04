package com.uncreated.civilized.core.building.requirement;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class EnclosedRoomRequirement implements IBuildingRequirement {

   public static final double VALID_WALL_HEIGHT = 3;
   public static final float PERCENTAGE_VALID_SOLID_WALL = 0.6f;

   public EnclosedRoomRequirement() {

   }

   @Override
   public RequirementKind kind() {
      return RequirementKind.STRUCTURAL;
   }

   @Override
   public IBuildingRequirementResult evaluate(RequirementContext context) {
      return getResult(context.level(), context.bounds());
   }

   public Result getResult(Level level, BuildingBounds bounds) {

      // the room is searched from the center, so a blocked center can't be checked. Clients check this before asking,
      // but the block may have changed since, and the server shouldn't throw on what a client sends it
      if (level.getBlockState(bounds.getCenter()).isSolid())
         return new Result(true);

      boolean leakFound = bfsCheckRoomsForLeaks(level, bounds.getCenter());

      return new Result(leakFound);
   }

   private static boolean isWall(BlockPos airBlockAboveFloor, Level level) {
      int wallBlocksFound = 0;
      BlockPos.MutableBlockPos current = airBlockAboveFloor.mutable();
      for (int y = 0; y < VALID_WALL_HEIGHT; y++) {
         // it's still fine to use isSolid() for this use case as this method works for chests/doors/panes
         if (level.getBlockState(current).isSolid())
            wallBlocksFound++;

         current.move(Direction.UP);
      }

      return wallBlocksFound / (float) VALID_WALL_HEIGHT > PERCENTAGE_VALID_SOLID_WALL;
   }

   public class Result implements IBuildingRequirementResult {
      private final boolean leakFound;

      private Result(boolean leakFound) {
         this.leakFound = leakFound;
      }

      @Override
      public boolean isSatisfied() {
         return !leakFound;
      }

      @Override
      public Component getDescription() {
         return Component.translatable("menu.building.management.requirements.enclosed_walls.description")
               .withColor(Colors.MENU_TEXT_DARK);
      }

      @Override
      public Component getTooltipDescription() {
         return Component.translatable("menu.building.management.requirements.enclosed_walls.tooltip");
      }

      @Override
      public boolean hideIfSatisfied() {
         return true;
      }
   }

   public boolean bfsCheckRoomsForLeaks(Level level, BlockPos startPos) {

      BlockState startState = level.getBlockState(startPos);
      if (startState.isSolid())
         throw new IllegalStateException("Cannot perform BFS leak check on building center that is a solid block.");

      // Direction vectors for traversing 4 directions
      Direction[] directions = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };

      Queue<BlockPos> queue = new LinkedList<>();
      queue.add(startPos);

      HashSet<BlockPos> visited = new HashSet<>();

      // a leak is defined as an empty or incomplete wall (i.e. a 1x1x3 that has <60% solid blocks) that is adjacent to
      // an outside (sky visible) block and can be reached from the center.
      boolean leakFound = false;

      // assume startPos is already valid
      visited.add(startPos);

      // BFS traversal
      while (!queue.isEmpty()) {
         BlockPos front = queue.poll();

         // Traverse all directions
         for (Direction direction : directions) {
            BlockPos nextPos = front.mutable().move(direction);

            boolean alreadyVisited = visited.contains(nextPos);

            boolean isOutside = level.canSeeSky(nextPos);

            if (!alreadyVisited) {

               visited.add(nextPos);

               boolean isWall = isWall(nextPos, level);
               // boolean isSolidBlock = level.getBlockState(nextPos).isSolid();

               boolean isValidInsideFloor = !isOutside && !isWall;
               if (isValidInsideFloor) {
                  queue.add(nextPos);

               }
            }

            if (isOutside) {
               leakFound = true;
            }
         }
      }

      return leakFound;
   }
}
