package com.uncreated.civilized.core.building.bounds.detection;

import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public record RoomDoor(BlockPos doorOrigin, BlockPos inside, BlockPos outside) {

   public static Optional<RoomDoor> fromDoorPos(BlockPos doorOrigin, BlockState blockState, Set<BlockPos> seen) {
      BlockPos pos = doorOrigin;
      BlockPos sideA;
      BlockPos sideB;

      if (blockState.is(BlockTags.TRAPDOORS)) {
         sideA = pos.above();
         sideB = pos.below();
      } else if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
         Direction facing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
         sideA = pos.relative(facing);
         sideB = pos.relative(facing.getOpposite());
      } else {
         // a modded door without a facing, so there's no telling which way it goes.
         return Optional.empty();
      }

      boolean aInside = seen.contains(sideA);
      boolean bInside = seen.contains(sideB);
      // not a valid room door if it leads somewhere we've already seen - this means the door doesn't actually go
      // anywhere
      if (aInside && bInside)
         return Optional.empty();

      // not a valid room door if for some reason we've never known either side of the door to be part of a room
      if (!aInside && !bInside)
         return Optional.empty();

      if (aInside)
         return Optional.of(new RoomDoor(pos, sideA, sideB));

      return Optional.of(new RoomDoor(pos, sideB, sideA));
   }
}
