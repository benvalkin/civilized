package com.uncreated.civilized.core.building.placement;

import com.uncreated.civilized.core.building.bounds.BuildingBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public sealed interface BuildingPlacement {

   /** Bounds the player dragged out themselves. */
   record Dragged(BuildingBounds bounds) implements BuildingPlacement {
   }

   /**
    * Automatically detected bounds from right-clicking.
    */
   record Detected(BlockPos start) implements BuildingPlacement {
   }

   StreamCodec<FriendlyByteBuf, BuildingPlacement> STREAM_CODEC =
         StreamCodec.of(BuildingPlacement::encode, BuildingPlacement::decode);

   private static void encode(FriendlyByteBuf buffer, BuildingPlacement placement) {
      switch (placement) {
      case Dragged dragged -> {
         buffer.writeBoolean(false);
         dragged.bounds().encode(buffer);
      }
      case Detected detected -> {
         buffer.writeBoolean(true);
         buffer.writeBlockPos(detected.start());
      }
      }
   }

   private static BuildingPlacement decode(FriendlyByteBuf buffer) {
      if (buffer.readBoolean())
         return new Detected(buffer.readBlockPos());
      return new Dragged(BuildingBounds.decode(buffer));
   }
}
