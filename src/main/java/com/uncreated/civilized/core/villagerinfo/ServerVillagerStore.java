package com.uncreated.civilized.core.villagerinfo;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.villagerinfo.events.model.VillagerInfoUpdatedEvent;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkills;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerVillagerStore extends VillagerStore {

   public static ServerVillagerStore INSTANCE;

   public static void loadServer(MinecraftServer server) {
      INSTANCE =
            server.overworld()
                  .getDataStorage()
                  .computeIfAbsent(
                        new SavedData.Factory<>(ServerVillagerStore::createDefault, ServerVillagerStore::load),
                        STORAGE_FILE_NAME);
   }

   public static final String STORAGE_FILE_NAME = "civilized_villagers";

   private static ServerVillagerStore createDefault() {
      return new ServerVillagerStore();
   }

   @Override
   public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {

      ListTag tags = new ListTag();
      for (VillagerInfo villagerInfo : villagers.all()) {
         CompoundTag item = villagerInfo.toNbt(registries);
         tags.add(item);
      }

      tag.put(STORAGE_FILE_NAME, tags);
      return tag;
   }

   // Load existing instance of saved data
   private static ServerVillagerStore load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
      ServerVillagerStore store = new ServerVillagerStore();

      ListTag list = tag.getList(STORAGE_FILE_NAME, Tag.TAG_COMPOUND);
      for (Tag i : list) {
         if (!(i instanceof CompoundTag itemTag)) {
            continue;
         }

         VillagerInfo villagerInfo = VillagerInfo.fromNbt(itemTag, lookupProvider);
         store.villagers.add(villagerInfo);
      }

      return store;
   }

   public void replicateChange(VillagerInfo villagerInfo, StoreOperation operation) {
      villagers.reindex(villagerInfo.getVillagerId());

      assert operation == StoreOperation.DELETE || villagers.exists(villagerInfo.getVillagerId());
      PacketDistributor.sendToAllPlayers(villagerInfo.toPacket(operation));
      NeoForge.EVENT_BUS.post(new VillagerInfoUpdatedEvent(villagerInfo, false));
   }

   public void replicateFullToNewClient(ServerPlayer player) {
      for (VillagerInfo villagerInfo : villagers.all()) {
         PacketDistributor.sendToPlayer(player, villagerInfo.toPacket(StoreOperation.INIT_NEW_CLIENT));
      }
   }

   public static void receiveSyncFromClient(VillagerInfo.Packet packet, IPayloadContext context) {

      VillagerInfo fromPacket = packet.villager();

      UUID key = fromPacket.getVillagerId();
      Optional<VillagerInfo> existing = INSTANCE.find(key);

      if (packet.storeOperation() == StoreOperation.ADD_OR_OVERWRITE
            || packet.storeOperation() == StoreOperation.INIT_NEW_CLIENT) {

         if (existing.isEmpty()) {
            INSTANCE.villagers.add(fromPacket);
         } else
            existing.get().copyFrom(fromPacket);

         INSTANCE.setDirty();
         INSTANCE.replicateChange(fromPacket, packet.storeOperation());
         return;
      }

      if ((packet.storeOperation() == StoreOperation.UPDATE || packet.storeOperation() == StoreOperation.DELETE)
            && existing.isEmpty()) {
         LOGGER.error(
               "Client {} tried to sync {} store operation for villager {} that did not exist on the server. This sync will be ignored.",
               packet.storeOperation(),
               context.player().getScoreboardName(),
               packet.villager().toStringLite());
         return;
      }

      if (packet.storeOperation() == StoreOperation.DELETE) {
         Optional<VillagerInfo> removed = INSTANCE.villagers.remove(existing.get().getVillagerId());
         if (removed.isPresent()) {
            INSTANCE.replicateChange(removed.get(), StoreOperation.DELETE);
            INSTANCE.setDirty();
         }
      } else if (packet.storeOperation() == StoreOperation.UPDATE) {
         existing.get().copyFrom(packet.villager());
         INSTANCE.replicateChange(existing.get(), StoreOperation.UPDATE);
         INSTANCE.setDirty();
      }
   }

   public VillagerInfo createNewVillager(CivilizedVillager villager) {
      VillagerInfo.VillagerInfoBuilder newVillager = VillagerInfo.builder().villagerId(UUID.randomUUID());
      Gender gender = villager.getRandom().nextBoolean() ? Gender.MALE : Gender.FEMALE;
      newVillager.gender(gender);
      Pair<String, String> names = VillagerInfo.generateRandomName(gender);
      newVillager.firstName(names.getFirst()).lastName(names.getSecond());
      newVillager.skills(VillagerSkills.randomSpread(villager.getRandom()));

      VillagerInfo info = newVillager.build();
      villagers.add(info);
      return info;
   }

   public void removeVillager(VillagerInfo info, boolean deceased) {
      villagers.remove(info.getVillagerId());
      info.setDeceased(deceased);
      setDirty();
      replicateChange(info, StoreOperation.DELETE);

      Stream.of(info.getHomeBuildingId(), info.getWorkBuildingId())
            .filter(Objects::nonNull)
            .distinct()
            .map(ServerBuildingsStore.INSTANCE::find)
            .flatMap(Optional::stream)
            .forEach(building -> {
               if (building.getOccupantIds().remove(info.getVillagerId()))
                  ServerBuildingsStore.INSTANCE.setDirty();
               ServerBuildingsStore.INSTANCE.replicateChange(building, StoreOperation.UPDATE);
            });

      find(info.getPartnerId()).ifPresent(partner -> {
         partner.setPartnerId(null);
         replicateChange(partner, StoreOperation.UPDATE);
      });
   }
}
