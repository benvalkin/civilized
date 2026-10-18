package com.uncreated.civilized.core.settlement.loadouts;

import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.production.bills.ItemFilters;
import com.uncreated.civilized.core.settlement.loadouts.events.LoadoutUpdatedEvent;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public class ServerLoadoutsStore extends LoadoutsStore {

   public static ServerLoadoutsStore INSTANCE;

   public static void loadServer(MinecraftServer server) {
      INSTANCE =
            server.overworld()
                  .getDataStorage()
                  .computeIfAbsent(
                        new Factory<>(ServerLoadoutsStore::createDefault, ServerLoadoutsStore::load),
                        STORAGE_FILE_NAME);
   }

   public static final String STORAGE_FILE_NAME = "civilized_loadout_store";

   private static final String FIELD_LOADOUT_ID = "loadout_id";
   private static final String FIELD_DISPLAY_NAME = "display_name";
   private static final String FIELD_ITEM_FILTERS = "item_filters";

   private static ServerLoadoutsStore createDefault() {
      return new ServerLoadoutsStore();
   }

   public boolean add(Loadout loadout) {
      Optional<Loadout> existing = loadouts.find(loadout.loadoutId());
      if (existing.isPresent()) {
         LOGGER.error(
               "Cannot add loadout '{}' - loadout with id {} already exists.",
               loadout.displayName(),
               loadout.loadoutId());
         return false;
      }

      loadouts.add(loadout);
      replicateChange(loadout, StoreOperation.ADD_OR_OVERWRITE);
      return true;
   }

   public void delete(UUID loadoutId) {
      loadouts.remove(loadoutId).ifPresent(loadout -> replicateChange(loadout, StoreOperation.DELETE));
   }

   private void replicateChange(Loadout loadout, StoreOperation operation) {
      setDirty();
      // todo: don't think we need to should client replication
      // PacketDistributor.sendToAllPlayers(loadout.toPacket(operation));
      NeoForge.EVENT_BUS.post(new LoadoutUpdatedEvent(loadout, false));
   }

   public void replicateFullToNewClient(ServerPlayer player) {
      for (Loadout loadout : loadouts.all())
         PacketDistributor.sendToPlayer(player, loadout.toPacket(StoreOperation.INIT_NEW_CLIENT));
   }

   @Override
   public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
      ListTag loadouts = new ListTag();
      for (Loadout loadout : this.loadouts.all()) {
         CompoundTag loadoutTag = new CompoundTag();
         loadoutTag.putUUID(FIELD_LOADOUT_ID, loadout.loadoutId());
         loadoutTag.putString(FIELD_DISPLAY_NAME, loadout.displayName());
         loadoutTag.put(FIELD_ITEM_FILTERS, loadout.loadoutItems().toNbt(provider));
         loadouts.add(loadoutTag);
      }

      tag.put(STORAGE_FILE_NAME, loadouts);
      return tag;
   }

   private static ServerLoadoutsStore load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
      ServerLoadoutsStore store = new ServerLoadoutsStore();

      for (Tag t : tag.getList(STORAGE_FILE_NAME, Tag.TAG_COMPOUND)) {
         if (!(t instanceof CompoundTag settlementTag))
            continue;

         UUID loadoutId = settlementTag.getUUID(FIELD_LOADOUT_ID);
         String displayName = settlementTag.getString(FIELD_DISPLAY_NAME);
         ItemFilters itemFilters =
               ItemFilters.fromNbt(settlementTag.getList(FIELD_ITEM_FILTERS, Tag.TAG_COMPOUND), lookupProvider);
         Loadout loadout = new Loadout(loadoutId, displayName, itemFilters);
         store.loadouts.add(loadout);
      }

      return store;
   }
}
