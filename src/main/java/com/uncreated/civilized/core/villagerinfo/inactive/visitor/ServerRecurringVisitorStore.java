package com.uncreated.civilized.core.villagerinfo.inactive.visitor;

import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.villagerinfo.inactive.ServerInactiveVillagerStore;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class ServerRecurringVisitorStore extends ServerInactiveVillagerStore<RecurringVisitorRecord, InactiveVisitorDB> {
   public static ServerRecurringVisitorStore INSTANCE;

   public static void loadServer(MinecraftServer server) {
      INSTANCE =
            server.overworld()
                  .getDataStorage()
                  .computeIfAbsent(
                        new SavedData.Factory<>(
                              ServerRecurringVisitorStore::createDefault,
                              ServerRecurringVisitorStore::load),
                        STORAGE_FILE_NAME);
   }

   public static final String STORAGE_FILE_NAME = "civilized_inactive_visitors";

   private static ServerRecurringVisitorStore createDefault() {
      return new ServerRecurringVisitorStore();
   }

   @Override
   protected @NotNull InactiveVisitorDB createDB() {
      return new InactiveVisitorDB();
   }

   private static ServerRecurringVisitorStore load(CompoundTag tag, HolderLookup.Provider registryAccess) {
      ServerRecurringVisitorStore store = new ServerRecurringVisitorStore();
      store.loadIntl(tag, registryAccess);
      return store;
   }

   @Override
   protected @NotNull RecurringVisitorRecord recordFromNbt(
         CompoundTag recordTag,
         HolderLookup.Provider registryAccess) {
      return new RecurringVisitorRecord(recordTag, registryAccess);
   }

   @Override
   public String getStorageFileName() {
      return STORAGE_FILE_NAME;
   }

   public Set<RecurringVisitorRecord> getForSettlement(UUID settlementId) {
      return villagers.getSettlementsToVillagersIndex().getValues(settlementId);
   }
}
