package com.uncreated.civilized.core.building.placement;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.bounds.detection.BoundsDetectionResult;
import com.uncreated.civilized.core.building.bounds.detection.BuildingDetectionType;
import com.uncreated.civilized.core.building.bounds.detection.Room;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public final class BuildingRoomDetection {

   private BuildingRoomDetection() {
   }

   public sealed interface Result {
   }

   public record Detected(BuildingBounds bounds, List<Room> rooms) implements Result {
   }

   public record Rejected(PlacementResult.Failure failure) implements Result {
   }

   public static Result detect(Level level, BuildingType buildingType, BlockPos start) {
      BuildingDetectionType detectionType = buildingType.detectionType();
      BoundsDetectionResult result = detectionType.createDetector().detect(start, level);

      return switch (detectionType) {
      case ROOMS -> detectRooms(buildingType, result);
      case PEN -> detectPen(result);
      case MANUAL_DRAG ->
         throw new IllegalArgumentException(buildingType + " does not support automatic bounds detection.");
      };
   }

   private static Result detectRooms(BuildingType buildingType, BoundsDetectionResult result) {
      Room mainRoom = result.mainRoom();

      if (mainRoom.originObstructed())
         return reject("message.building.placement.detection.start_obstructed", null);

      // the clicked spot itself is outside, so there's no building there to find
      if (mainRoom.passableBlocks().isEmpty())
         return reject("message.building.placement.detection.start_outside", null);

      if (mainRoom.tooLarge())
         return reject("message.building.placement.detection.indoor.too_large", null);

      List<Room> rooms;
      if (requiresEnclosure(buildingType)) {
         if (!mainRoom.enclosed()) {
            BlockPos leak = mainRoom.leak();
            return reject(
                  "message.building.placement.detection.indoor.not_enclosed",
                  leak,
                  leak != null ? leak.toShortString() : "?");
         }

         rooms = result.enclosedRooms();
      } else {
         // only the room that was clicked in has to be roofed, e.g. an open-sided storehouse. Enclosed rooms through
         // its doors are still part of it
         rooms = Stream.concat(Stream.of(mainRoom), result.enclosedRooms().stream()).distinct().toList();
      }

      return new Detected(result.createBounds(rooms), rooms);
   }

   private static Result detectPen(BoundsDetectionResult result) {
      Room pen = result.mainRoom();

      if (pen.originObstructed())
         return reject("message.building.placement.detection.start_obstructed", null);

      if (!pen.enclosed())
         return reject("message.building.placement.detection.pen.not_enclosed", pen.leak());

      return new Detected(result.createBounds(List.of(pen)), List.of(pen));
   }

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
