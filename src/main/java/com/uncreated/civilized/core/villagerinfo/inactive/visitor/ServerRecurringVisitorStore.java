package com.uncreated.civilized.core.villagerinfo.inactive.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.recruitment.VisitorHospitality;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.inactive.ServerInactiveVillagerStore;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

public class ServerRecurringVisitorStore
      extends ServerInactiveVillagerStore<RecurringVisitorRecord, InactiveVisitorDB> {
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

   public static final int MAX_INACTIVE_VISITORS_PER_SETTLEMENT = 7;
   private static final long MIN_DAYS_AWAY = 3;
   private static final long MAX_DAYS_AWAY = 20;

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

   public void remember(VillagerInfo visitor, VisitorHospitality hospitality, int visits, long today) {
      this.add(new RecurringVisitorRecord(visitor, today, hospitality.getPoints(), visits));

      forgetLeastImpressedOverCap(visitor.getVisitingSettlementId());
   }

   public void forgetLeastImpressedOverCap(UUID settlementId) {
      List<RecurringVisitorRecord> remembered = new ArrayList<>(this.getForSettlement(settlementId));
      if (remembered.size() <= MAX_INACTIVE_VISITORS_PER_SETTLEMENT)
         return;

      RecurringVisitorRecord leastImpressed = remembered.getFirst();
      for (RecurringVisitorRecord record : remembered) {
         if (record.getAccruedHospitality() < leastImpressed.getAccruedHospitality())
            leastImpressed = record;
      }
      this.remove(leastImpressed.getInfo().getVillagerId());
   }

   public Optional<RecurringVisitorRecord> findRandomReturningVisitor(
         UUID settlementId,
         long today,
         RandomSource random) {
      List<RecurringVisitorRecord> candidates = new ArrayList<>();
      // copied, since forgetting a visitor removes them from the index
      for (RecurringVisitorRecord record : List.copyOf(this.getForSettlement(settlementId))) {
         long daysAway = today - record.getDepartedDay();
         // a negative number of days means the clock was set back
         if (daysAway < 0 || daysAway > MAX_DAYS_AWAY)
            this.remove(record.getInfo().getVillagerId());
         else if (daysAway >= MIN_DAYS_AWAY)
            candidates.add(record);
      }

      if (candidates.isEmpty())
         return Optional.empty();

      return Optional.of(candidates.get(random.nextInt(candidates.size())));
   }
}
