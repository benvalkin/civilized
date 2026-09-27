package com.uncreated.civilized.core.settlement.permission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.settlement.permission.events.SettlementPermissionsUpdatedEvent;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public class ServerSettlementPermissionStore extends SettlementPermissionStore {

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

   private static ServerSettlementPermissionStore createDefault() {
      return new ServerSettlementPermissionStore();
   }

   public SettlementPermissions getOrCreate(UUID settlementId) {
      Optional<SettlementPermissions> existing = permissions.find(settlementId);
      if (existing.isPresent())
         return existing.get();

      SettlementPermissions created = new SettlementPermissions(settlementId, List.of());
      permissions.add(created);
      replicateChange(created, StoreOperation.ADD_OR_OVERWRITE);
      return created;
   }

   public void setAccessLevel(UUID settlementId, Player player, AccessLevel accessLevel) {
      SettlementPermissions settlementPermissions = getOrCreate(settlementId);
      settlementPermissions.setAccessLevel(player, accessLevel);
      replicateChange(settlementPermissions, StoreOperation.ADD_OR_OVERWRITE);
   }

   public void setAccessLevel(UUID settlementId, UUID playerId, String scoreboardName, AccessLevel accessLevel) {
      SettlementPermissions settlementPermissions = getOrCreate(settlementId);
      settlementPermissions.setAccessLevel(playerId, scoreboardName, accessLevel);
      replicateChange(settlementPermissions, StoreOperation.ADD_OR_OVERWRITE);
   }

   public void removeAccess(UUID settlementId, UUID playerId) {
      find(settlementId).ifPresent(settlementPermissions -> {
         settlementPermissions.removeAccess(playerId);
         replicateChange(settlementPermissions, StoreOperation.ADD_OR_OVERWRITE);
      });
   }

   public void delete(UUID settlementId) {
      permissions.remove(settlementId)
            .ifPresent(settlementPermissions -> replicateChange(settlementPermissions, StoreOperation.DELETE));
   }

   private void replicateChange(SettlementPermissions settlementPermissions, StoreOperation operation) {
      setDirty();
      PacketDistributor.sendToAllPlayers(settlementPermissions.toPacket(operation));
      NeoForge.EVENT_BUS.post(new SettlementPermissionsUpdatedEvent(settlementPermissions, false));
   }

   public void replicateFullToNewClient(ServerPlayer player) {
      for (SettlementPermissions settlementPermissions : permissions.all())
         PacketDistributor.sendToPlayer(player, settlementPermissions.toPacket(StoreOperation.INIT_NEW_CLIENT));
   }

   @Override
   public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
      ListTag settlements = new ListTag();
      for (SettlementPermissions settlementPermissions : permissions.all()) {
         CompoundTag settlementTag = new CompoundTag();
         settlementTag.putUUID(FIELD_SETTLEMENT_ID, settlementPermissions.settlementId());

         settlementTag.put(FIELD_LIST_PLAYERS, PlayerPermission.toNbtList(settlementPermissions.entries()));

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

         List<PlayerPermission> players =
               PlayerPermission.fromNbtList(settlementTag.getList(FIELD_LIST_PLAYERS, Tag.TAG_COMPOUND));
         store.permissions.add(new SettlementPermissions(settlementTag.getUUID(FIELD_SETTLEMENT_ID), players));
      }

      return store;
   }
}
