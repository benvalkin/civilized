package com.uncreated.civilized.core.building.placement;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.bounds.detection.BoundsDetectionResult;
import com.uncreated.civilized.core.building.bounds.detection.BoundsDetector;
import com.uncreated.civilized.core.building.bounds.detection.Room;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** Works out a building's bounds from its rooms, as detected from where the player clicked inside it. */
public final class BuildingRoomDetection {

   private BuildingRoomDetection() {
   }

   public sealed interface Result {
   }

   /** @param rooms the rooms the building is made of */
   public record Detected(BuildingBounds bounds, List<Room> rooms) implements Result {
   }

   public record Rejected(PlacementResult.Failure failure) implements Result {
   }

   public static Result detect(Level level, BuildingType buildingType, BlockPos start) {
      BoundsDetectionResult result = new BoundsDetector().detect(start, level);
      Room mainRoom = result.mainRoom();

      if (mainRoom.originObstructed())
         return reject("message.building.placement.detection.start_obstructed", null);

      // the clicked spot itself is outside, so there's no building there to find
      if (mainRoom.passableBlocks().isEmpty())
         return reject("message.building.placement.detection.start_outside", null);

      if (mainRoom.tooLarge())
         return reject("message.building.placement.detection.too_large", null);

      List<Room> rooms;
      if (requiresEnclosure(buildingType)) {
         if (!mainRoom.enclosed()) {
            BlockPos leak = mainRoom.leak();
            return reject(
                  "message.building.placement.detection.not_enclosed",
                  leak,
                  leak != null ? leak.toShortString() : "?");
         }

         rooms = result.enclosedRooms();
      } else {
         // only the room that was clicked in has to be roofed, e.g. an open-sided storehouse. Enclosed rooms through its
         // doors are still part of it
         rooms = Stream.concat(Stream.of(mainRoom), result.enclosedRooms().stream()).distinct().toList();
      }

      return new Detected(result.createBounds(rooms), rooms);
   }

   /** Some buildings only need a roof over them, rather than walls all the way around. */
   private static boolean requiresEnclosure(BuildingType buildingType) {
      return !buildingType.is(BuildingTypes.STOREHOUSE);
   }

   private static Rejected reject(String translationKey, @Nullable BlockPos highlight, Object... args) {
      return new Rejected(
            new PlacementResult.Failure(
                  Component.translatable(translationKey, args).withColor(Colors.VALIDATION_ERROR),
                  highlight));
   }
}
