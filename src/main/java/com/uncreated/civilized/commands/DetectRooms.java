package com.uncreated.civilized.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.bounds.detection.BoundsDetectionResult;
import com.uncreated.civilized.core.building.bounds.detection.BoundsDetector;
import com.uncreated.civilized.core.building.bounds.detection.Room;
import com.uncreated.civilized.core.building.bounds.detection.RoomDoor;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CivilizedMod.CIVILIZED_MOD_ID)
public class DetectRooms {

   private static final double LOOK_DISTANCE = 20;
   private static final int DISPLAY_DURATION_TICKS = 30 * 20;
   /** Particles only last so long, so they're shown again at this interval. */
   private static final int REDRAW_INTERVAL_TICKS = 10;
   /** Keeps huge rooms from flooding the player with particles. */
   private static final int MAX_MARKERS = 3000;
   private static final double OUTLINE_SPACING = 0.5;

   private static final DustParticleOptions ENCLOSED_FLOOR = new DustParticleOptions(0x55FF55, 1.0F);
   private static final DustParticleOptions UNENCLOSED_FLOOR = new DustParticleOptions(0xFFAA00, 1.0F);
   private static final DustParticleOptions DOOR = new DustParticleOptions(0xFFFF55, 1.5F);
   private static final DustParticleOptions LEAK = new DustParticleOptions(0xFF2222, 2.5F);
   private static final DustParticleOptions ROOM_ORIGIN = new DustParticleOptions(0x5555FF, 2.0F);
   private static final DustParticleOptions BOUNDS = new DustParticleOptions(0xFFFFFF, 0.7F);

   private static final SimpleCommandExceptionType ERROR_NOT_PLAYER =
         new SimpleCommandExceptionType(Component.literal("This command can only be used by players."));
   private static final SimpleCommandExceptionType ERROR_NOT_LOOKING_AT_BLOCK =
         new SimpleCommandExceptionType(Component.literal("Look at a block inside the building, e.g. its floor."));

   private record Marker(Vec3 pos, DustParticleOptions particle) {
   }

   private record Display(ResourceKey<Level> dimension, List<Marker> markers, long expiresAt) {
   }

   /** What each player is being shown, by player ID. */
   private static final Map<UUID, Display> DISPLAYS = new HashMap<>();

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
            Commands.literal("detectrooms")
                  .requires(source -> source.hasPermission(2))
                  .executes(context -> detect(context.getSource()))
                  .then(Commands.literal("clear").executes(context -> clear(context.getSource()))));
   }

   private static int detect(CommandSourceStack source) throws CommandSyntaxException {
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
      BoundsDetectionResult result = new BoundsDetector().detect(start, level);
      double tookMs = (System.nanoTime() - startedAt) / 1_000_000.0;

      // too large rooms aren't in the result's rooms, but the main room being too large is worth knowing about
      List<Room> rooms = new ArrayList<>(result.rooms());
      if (!rooms.contains(result.mainRoom()))
         rooms.addFirst(result.mainRoom());

      List<Room> enclosedRooms = result.enclosedRooms();
      BuildingBounds bounds = enclosedRooms.stream().anyMatch(room -> !room.passableBlocks().isEmpty())
            ? result.createBounds(enclosedRooms)
            : null;

      List<Marker> markers = createMarkers(rooms, bounds);
      DISPLAYS.put(
            player.getUUID(),
            new Display(level.dimension(), markers, level.getGameTime() + DISPLAY_DURATION_TICKS));

      sendSummary(source, start, rooms, result.mainRoom(), bounds, tookMs);
      return rooms.size();
   }

   private static int clear(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer player = source.getPlayer();
      if (player == null)
         throw ERROR_NOT_PLAYER.create();

      DISPLAYS.remove(player.getUUID());
      source.sendSuccess(() -> Component.literal("Cleared the room detection display."), false);
      return 1;
   }

   private static List<Marker> createMarkers(List<Room> rooms, BuildingBounds bounds) {
      List<Marker> markers = new ArrayList<>();
      for (Room room : rooms) {
         DustParticleOptions floor = room.enclosed() ? ENCLOSED_FLOOR : UNENCLOSED_FLOOR;
         for (BlockPos pos : room.validFloorBlocks())
            markers.add(new Marker(new Vec3(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5), floor));

         for (RoomDoor door : room.doors())
            markers.add(new Marker(door.doorOrigin().getCenter(), DOOR));

         markers.add(new Marker(room.origin().getCenter(), ROOM_ORIGIN));
         if (room.leak() != null)
            markers.add(new Marker(room.leak().getCenter(), LEAK));
      }

      if (bounds != null)
         addOutline(markers, bounds);

      return markers.size() > MAX_MARKERS ? markers.subList(0, MAX_MARKERS) : markers;
   }

   /** Dots along the 12 edges of the bounds. */
   private static void addOutline(List<Marker> markers, BuildingBounds bounds) {
      double minX = bounds.getLowerCorner().getX(), maxX = bounds.getUpperCorner().getX() + 1;
      double minY = bounds.getLowerCorner().getY(), maxY = bounds.getUpperCorner().getY() + 1;
      double minZ = bounds.getLowerCorner().getZ(), maxZ = bounds.getUpperCorner().getZ() + 1;

      for (double x = minX; x <= maxX; x += OUTLINE_SPACING)
         for (double y : new double[] { minY, maxY })
            for (double z : new double[] { minZ, maxZ })
               markers.add(new Marker(new Vec3(x, y, z), BOUNDS));
      for (double y = minY; y <= maxY; y += OUTLINE_SPACING)
         for (double x : new double[] { minX, maxX })
            for (double z : new double[] { minZ, maxZ })
               markers.add(new Marker(new Vec3(x, y, z), BOUNDS));
      for (double z = minZ; z <= maxZ; z += OUTLINE_SPACING)
         for (double x : new double[] { minX, maxX })
            for (double y : new double[] { minY, maxY })
               markers.add(new Marker(new Vec3(x, y, z), BOUNDS));
   }

   private static void sendSummary(
         CommandSourceStack source,
         BlockPos start,
         List<Room> rooms,
         Room mainRoom,
         BuildingBounds bounds,
         double tookMs) {
      long enclosed = rooms.stream().filter(Room::enclosed).filter(room -> !room.tooLarge()).count();
      source.sendSuccess(
            () -> Component.literal(
                  String.format(
                        "Detected %d room(s) from %s in %.1fms, %d of them enclosed:",
                        rooms.size(),
                        start.toShortString(),
                        tookMs,
                        enclosed)),
            false);

      for (int i = 0; i < rooms.size(); i++) {
         Room room = rooms.get(i);
         String label = room == mainRoom ? "main" : "#" + i;
         source.sendSuccess(() -> describeRoom(label, room), false);
      }

      if (bounds != null) {
         BlockPos size = bounds.getUpperCorner().subtract(bounds.getLowerCorner()).offset(1, 1, 1);
         source.sendSuccess(
               () -> Component.literal(
                     String.format(
                           "Bounds of the enclosed rooms: %dx%dx%d, from %s to %s",
                           size.getX(),
                           size.getY(),
                           size.getZ(),
                           bounds.getLowerCorner().toShortString(),
                           bounds.getUpperCorner().toShortString())),
               false);
      }

      source.sendSuccess(
            () -> Component
                  .literal(
                        "Green: enclosed floor, orange: unenclosed floor, yellow: door, red: leak, blue: room start, white: bounds")
                  .withStyle(ChatFormatting.GRAY),
            false);
   }

   private static Component describeRoom(String label, Room room) {
      String status;
      ChatFormatting colour;
      if (room.originObstructed()) {
         status = "starts inside a block";
         colour = ChatFormatting.RED;
      } else if (room.tooLarge()) {
         status = "too large";
         colour = ChatFormatting.RED;
      } else if (!room.enclosed()) {
         status = room.leak() != null ? "leaks at " + room.leak().toShortString() : "not enclosed";
         colour = ChatFormatting.GOLD;
      } else {
         status = "enclosed";
         colour = ChatFormatting.GREEN;
      }

      return Component.literal(
            String.format(
                  " %s at %s: %s, %d blocks, %d floor, %d door(s)",
                  label,
                  room.origin().toShortString(),
                  status,
                  room.passableBlocks().size(),
                  room.validFloorBlocks().size(),
                  room.doors().size()))
            .withStyle(colour);
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent.Post event) {
      if (DISPLAYS.isEmpty())
         return;

      DISPLAYS.entrySet().removeIf(entry -> {
         ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
         if (player == null)
            return true;

         Display display = entry.getValue();
         ServerLevel level = player.serverLevel();
         if (!level.dimension().equals(display.dimension()) || level.getGameTime() >= display.expiresAt())
            return true;

         if (level.getGameTime() % REDRAW_INTERVAL_TICKS == 0)
            for (Marker marker : display.markers())
               level.sendParticles(
                     player,
                     marker.particle(),
                     true,
                     true,
                     marker.pos().x,
                     marker.pos().y,
                     marker.pos().z,
                     1,
                     0,
                     0,
                     0,
                     0);
         return false;
      });
   }
}
