package com.uncreated.civilized.core.settlement;

import java.util.Random;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Getter
public class SettlementBounds {

   private final Random random = new Random();

   public static SettlementBounds decode(FriendlyByteBuf buffer) {
      return new SettlementBounds(buffer.readBlockPos(), buffer.readBlockPos(), buffer.readBlockPos());
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeBlockPos(origin);
      buffer.writeBlockPos(lowerCorner);
      buffer.writeBlockPos(upperCorner);
   }

   private final BlockPos origin;
   private final BlockPos lowerCorner;
   private final BlockPos upperCorner;
   private final AABB encapsulatingAABB;

   public static int SETTLEMENT_MIN_Y = 52;
   public static int SETTLEMENT_MAX_Y = 192;
   public static int SETTLEMENT_XZ_MARGIN = 32;

   public SettlementBounds(BlockPos origin) {
      this.origin = origin;
      this.lowerCorner = origin.offset(-SETTLEMENT_XZ_MARGIN, -SETTLEMENT_MIN_Y, -SETTLEMENT_XZ_MARGIN);
      this.upperCorner = origin.offset(SETTLEMENT_XZ_MARGIN, SETTLEMENT_MAX_Y, SETTLEMENT_XZ_MARGIN);
      encapsulatingAABB = AABB.encapsulatingFullBlocks(this.lowerCorner, this.upperCorner);
   }

   public SettlementBounds(BlockPos origin, BlockPos lowerCorner, BlockPos upperCorner) {
      this.origin = origin;
      this.lowerCorner = lowerCorner;
      this.upperCorner = upperCorner;
      encapsulatingAABB = AABB.encapsulatingFullBlocks(this.lowerCorner, this.upperCorner);
   }

   public static SettlementBounds fromBuildingCorners(BlockPos origin, BlockPos minBuildingCorner, BlockPos maxBuildingCorner) {
      return new SettlementBounds(origin,
      minBuildingCorner.offset(-SETTLEMENT_XZ_MARGIN, -SETTLEMENT_MIN_Y, -SETTLEMENT_XZ_MARGIN),
      maxBuildingCorner.offset(SETTLEMENT_XZ_MARGIN, SETTLEMENT_MAX_Y, SETTLEMENT_XZ_MARGIN));
   }

   public boolean contains(BlockPos blockPos) {
      return encapsulatingAABB.contains(blockPos.getCenter());
   }

   public boolean contains(Vec3 pos) {
      return encapsulatingAABB.contains(pos);
   }

   public boolean isOverlapping(SettlementBounds other) {
      return encapsulatingAABB.intersects(other.getEncapsulatingAABB());
   }

   public int getDirectionBound(Direction direction) {
      return switch (direction) {
      case EAST -> upperCorner.getX();
      case WEST -> lowerCorner.getX();
      case SOUTH -> upperCorner.getZ();
      case NORTH -> lowerCorner.getZ();
      case UP -> upperCorner.getY();
      case DOWN -> lowerCorner.getY();
      };
   }

   @Override
   public String toString() {
      return String.format("Center: %s LowerCorner: %s UpperCorner: %s", origin, lowerCorner, upperCorner);
   }
}
