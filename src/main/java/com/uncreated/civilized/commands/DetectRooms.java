package com.uncreated.civilized.commands;

import static com.uncreated.civilized.core.building.bounds.detection.BuildingDetectionType.PEN;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.bounds.detection.BoundsDetectionResult;
import com.uncreated.civilized.core.building.bounds.detection.BoundsDetector;
import com.uncreated.civilized.core.building.bounds.detection.BuildingDetectionType;
import com.uncreated.civilized.core.building.bounds.detection.Room;
import com.uncreated.civilized.networking.packets.VisualizeBoundsInstruction;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;

public class DetectRooms {

   private static final double LOOK_DISTANCE = 20;

   private static final SimpleCommandExceptionType ERROR_NOT_PLAYER =
         new SimpleCommandExceptionType(Component.literal("This command can only be used by players."));
   private static final SimpleCommandExceptionType ERROR_NOT_LOOKING_AT_BLOCK =
         new SimpleCommandExceptionType(Component.literal("Look at a block inside the building, e.g. its floor."));

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> command =
            Commands.literal("detectrooms")
                  .requires(source -> source.hasPermission(2))
                  .executes(context -> detect(context.getSource(), BuildingDetectionType.ROOMS))
                  .then(Commands.literal("clear").executes(context -> clear(context.getSource())));

      for (BuildingDetectionType type : BuildingDetectionType.values()) {
         // dragged bounds do not require any detection
         if (!type.autoDetected())
            continue;

         command.then(
               Commands.literal(type.name().toLowerCase(Locale.ROOT))
                     .executes(context -> detect(context.getSource(), type)));
      }

      dispatcher.register(command);
   }

   private static int detect(CommandSourceStack source, BuildingDetectionType type) throws CommandSyntaxException {
      ServerPlayer player = source.getPlayer();
      if (player == null)
         throw ERROR_NOT_PLAYER.create();

      HitResult hit = player.pick(LOOK_DISTANCE, 1.0F, false);
      if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK)
         throw ERROR_NOT_LOOKING_AT_BLOCK.create();

      // the block in front of the face that was looked at, e.g. the air above the floor
      BlockPos start = blockHit.getBlockPos().relative(blockHit.getDirection());
      ServerLevel level = player.serverLevel();

      long startedAt = System.nanoTime();
      BoundsDetector detector = type.createDetector();
      BoundsDetectionResult result = detector.detect(start, level);
      double tookMs = (System.nanoTime() - startedAt) / 1_000_000.0;

      // too large rooms aren't in the result's rooms, but the main room being too large is worth knowing about
      List<Room> rooms = new ArrayList<>(result.rooms());
      if (!rooms.contains(result.mainRoom()))
         rooms.addFirst(result.mainRoom());

      List<Room> enclosedRooms = result.enclosedRooms();
      BuildingBounds bounds =
            enclosedRooms.stream().anyMatch(room -> !room.passableBlocks().isEmpty())
                  ? result.createBounds(enclosedRooms)
                  : null;

      // the client draws what was found, for a while
      PacketDistributor.sendToPlayer(
            player,
            new VisualizeBoundsInstruction(VisualizeBoundsInstruction.allMarkerTypes(rooms, type), Optional.ofNullable(bounds)));


      sendSummary(source, start, rooms, result.mainRoom(), bounds, tookMs, type);
      return rooms.size();
   }

   private static int clear(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer player = source.getPlayer();
      if (player == null)
         throw ERROR_NOT_PLAYER.create();

      PacketDistributor.sendToPlayer(player, VisualizeBoundsInstruction.CLEAR);
      source.sendSuccess(() -> Component.literal("Cleared the room detection display."), false);
      return 1;
   }

   private static void sendSummary(
         CommandSourceStack source,
         BlockPos start,
         List<Room> rooms,
         Room mainRoom,
         BuildingBounds bounds,
         double tookMs,
         BuildingDetectionType type) {
      long enclosed = rooms.stream().filter(Room::enclosed).filter(room -> !room.tooLarge()).count();
      String heading =
            type == PEN ? String.format("Detected a pen from %s in %.1fms:", start.toShortString(), tookMs)
                  : String.format(
                        "Detected %d room(s) from %s in %.1fms, %d of them enclosed:",
                        rooms.size(),
                        start.toShortString(),
                        tookMs,
                        enclosed);
      source.sendSuccess(() -> Component.literal(heading), false);

      for (int i = 0; i < rooms.size(); i++) {
         Room room = rooms.get(i);
         String label = type == PEN ? "pen" : room == mainRoom ? "main" : "#" + i;
         source.sendSuccess(() -> describeRoom(label, room, type), false);
      }

      if (bounds != null) {
         BlockPos size = bounds.getUpperCorner().subtract(bounds.getLowerCorner()).offset(1, 1, 1);
         source.sendSuccess(
               () -> Component.literal(
                     String.format(
                           "Bounds of the %s: %dx%dx%d, from %s to %s",
                           type == PEN ? "pen" : "enclosed rooms",
                           size.getX(),
                           size.getY(),
                           size.getZ(),
                           bounds.getLowerCorner().toShortString(),
                           bounds.getUpperCorner().toShortString())),
               false);
      }

      String legend =
            type == PEN
                  ? "Green: enclosed floor, orange: unenclosed floor, grey: fences and walls, yellow: gate, red: leak, blue: start, white: bounds"
                  : "Green: enclosed floor, orange: unenclosed floor, yellow: door, red: leak, blue: room start, white: bounds";
      source.sendSuccess(() -> Component.literal(legend).withStyle(ChatFormatting.GRAY), false);
   }

   private static Component describeRoom(String label, Room room, BuildingDetectionType type) {
      String status;
      ChatFormatting colour;
      if (room.originObstructed()) {
         status = type == PEN ? "starts somewhere animals can't stand" : "starts inside a block";
         colour = ChatFormatting.RED;
      } else if (room.tooLarge()) {
         status = "too large";
         colour = ChatFormatting.RED;
      } else if (!room.enclosed()) {
         if (room.leak() != null)
            status = "leaks at " + room.leak().toShortString();
         else
            // a pen's search only runs out of room to explore when it's fenced in
            status = type == PEN ? "not fenced in, or too large" : "not enclosed";
         colour = ChatFormatting.GOLD;
      } else {
         status = "enclosed";
         colour = ChatFormatting.GREEN;
      }

      return Component
            .literal(
                  String.format(
                        " %s at %s: %s, %d blocks, %d floor, %d %s",
                        label,
                        room.origin().toShortString(),
                        status,
                        room.passableBlocks().size(),
                        room.validFloorBlocks().size(),
                        room.doors().size(),
                        type == PEN ? "gate(s)" : "door(s)"))
            .withStyle(colour);
   }
}
