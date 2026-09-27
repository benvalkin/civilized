package com.uncreated.civilized.core.settlement.permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

public record PlayerPermission(UUID playerId, String scoreboardName, AccessLevel accessLevel) {

   private static final Logger LOGGER = LogUtils.getLogger();

   private static final String FIELD_PLAYER_ID = "player_id";
   private static final String FIELD_SCOREBOARD_NAME = "scoreboard_name";
   private static final String FIELD_ACCESS_LEVEL = "access_level";

   public static final StreamCodec<FriendlyByteBuf, PlayerPermission> STREAM_CODEC =
         StreamCodec.composite(
               UUIDUtil.STREAM_CODEC,
               PlayerPermission::playerId,
               ByteBufCodecs.STRING_UTF8,
               PlayerPermission::scoreboardName,
               NeoForgeStreamCodecs.enumCodec(AccessLevel.class),
               PlayerPermission::accessLevel,
               PlayerPermission::new);

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      tag.putUUID(FIELD_PLAYER_ID, playerId);
      tag.putString(FIELD_SCOREBOARD_NAME, scoreboardName);
      tag.putString(FIELD_ACCESS_LEVEL, accessLevel.name());
      return tag;
   }

   public static Optional<PlayerPermission> fromNbt(CompoundTag tag) {
      String accessLevelName = tag.getString(FIELD_ACCESS_LEVEL);
      try {
         AccessLevel accessLevel = AccessLevel.valueOf(accessLevelName);
         return Optional.of(
               new PlayerPermission(tag.getUUID(FIELD_PLAYER_ID), tag.getString(FIELD_SCOREBOARD_NAME), accessLevel));
      } catch (IllegalArgumentException ex) {
         LOGGER.warn("Ignoring unknown settlement access level '{}'", accessLevelName);
         return Optional.empty();
      }
   }

   public static ListTag toNbtList(Collection<PlayerPermission> permissions) {
      ListTag list = new ListTag();
      for (PlayerPermission permission : permissions)
         list.add(permission.toNbt());
      return list;
   }

   public static List<PlayerPermission> fromNbtList(ListTag list) {
      List<PlayerPermission> permissions = new ArrayList<>();
      for (Tag tag : list) {
         if (tag instanceof CompoundTag compoundTag)
            fromNbt(compoundTag).ifPresent(permissions::add);
      }
      return permissions;
   }
}
