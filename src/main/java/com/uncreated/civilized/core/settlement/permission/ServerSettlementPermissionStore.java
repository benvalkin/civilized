package com.uncreated.civilized.core.settlement.permission;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

public class ServerSettlementPermissionStore extends SavedData {

   private static final Logger LOGGER = LogUtils.getLogger();

   public static ServerSettlementPermissionStore INSTANCE;

   public static void loadServer(MinecraftServer server) {
      INSTANCE =
            server.overworld()
                  .getDataStorage()
                  .computeIfAbsent(
                        new SavedData.Factory<>(
                              ServerSettlementPermissionStore::createDefault,
                              ServerSettlementPermissionStore::load),
                        STORAGE_FILE_NAME);
   }

   public static final String STORAGE_FILE_NAME = "civilized_settlement_permissions";

   private static final String FIELD_SETTLEMENT_ID = "settlement_id";
   private static final String FIELD_LIST_PLAYERS = "list_players";
   private static final String FIELD_PLAYER_ID = "player_id";
   private static final String FIELD_SCOREBOARD_NAME = "scoreboard_name";
   private static final String FIELD_ACCESS_LEVEL = "access_level";

   private static ServerSettlementPermissionStore createDefault() {
      return new ServerSettlementPermissionStore();
   }

   private final SettlementPermissionsDB permissions = new SettlementPermissionsDB();

   public Optional<SettlementPermissions> find(UUID settlementId) {
      return permissions.find(settlementId);
   }

   public SettlementPermissions getOrCreate(UUID settlementId) {
      Optional<SettlementPermissions> existing = permissions.find(settlementId);
      if (existing.isPresent())
         return existing.get();

      SettlementPermissions created = new SettlementPermissions(settlementId, List.of());
      permissions.add(created);
      setDirty();
      return created;
   }

   public void setAccessLevel(UUID settlementId, Player player, AccessLevel accessLevel) {
      getOrCreate(settlementId).setAccessLevel(player, accessLevel);
      setDirty();
   }

   public void removeAccess(UUID settlementId, UUID playerId) {
      find(settlementId).ifPresent(p -> {
         p.removeAccess(playerId);
         setDirty();
      });
   }

   public void delete(UUID settlementId) {
      if (permissions.remove(settlementId).isPresent())
         setDirty();
   }

   @Override
   public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
      ListTag settlements = new ListTag();
      for (SettlementPermissions settlementPermissions : permissions.all()) {
         CompoundTag settlementTag = new CompoundTag();
         settlementTag.putUUID(FIELD_SETTLEMENT_ID, settlementPermissions.settlementId());

         ListTag players = new ListTag();
         for (PlayerPermission permission : settlementPermissions.entries()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID(FIELD_PLAYER_ID, permission.playerId());
            playerTag.putString(FIELD_SCOREBOARD_NAME, permission.scoreboardName());
            playerTag.putString(FIELD_ACCESS_LEVEL, permission.accessLevel().name());
            players.add(playerTag);
         }
         settlementTag.put(FIELD_LIST_PLAYERS, players);

         settlements.add(settlementTag);
      }

      tag.put(STORAGE_FILE_NAME, settlements);
      return tag;
   }

   private static ServerSettlementPermissionStore load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
      ServerSettlementPermissionStore store = new ServerSettlementPermissionStore();

      for (Tag t : tag.getList(STORAGE_FILE_NAME, Tag.TAG_COMPOUND)) {
         if (!(t instanceof CompoundTag settlementTag))
            continue;

         List<PlayerPermission> players = new ArrayList<>();
         for (Tag p : settlementTag.getList(FIELD_LIST_PLAYERS, Tag.TAG_COMPOUND)) {
            if (!(p instanceof CompoundTag playerTag))
               continue;

            loadAccessLevel(playerTag.getString(FIELD_ACCESS_LEVEL)).ifPresent(
                  accessLevel -> players.add(
                        new PlayerPermission(
                              playerTag.getUUID(FIELD_PLAYER_ID),
                              playerTag.getString(FIELD_SCOREBOARD_NAME),
                              accessLevel)));
         }

         store.permissions.add(new SettlementPermissions(settlementTag.getUUID(FIELD_SETTLEMENT_ID), players));
      }

      return store;
   }

   private static Optional<AccessLevel> loadAccessLevel(String name) {
      try {
         return Optional.of(AccessLevel.valueOf(name));
      } catch (IllegalArgumentException ex) {
         LOGGER.warn("Ignoring unknown settlement access level '{}'", name);
         return Optional.empty();
      }
   }
}
