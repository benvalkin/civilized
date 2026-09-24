package com.uncreated.civilized.core.villagerinfo;

import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.villagerinfo.events.model.VillagerInfoUpdatedEvent;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientVillagerStore extends VillagerStore {

   // set the instance as early as possible because the server could sync changes when the client joins
   public static ClientVillagerStore INSTANCE = new ClientVillagerStore();

   private ClientVillagerStore() {
      super();
   }

   public static void loadClient() {
      // new instance in case the client is rejoining (the old instance might still have data in it)
      INSTANCE = new ClientVillagerStore();
   }

   @Override
   public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
      throw new UnsupportedOperationException("Saving not supported on client.");
   }

   public void replicateChange(VillagerInfo villagerInfo, StoreOperation operation) {
      if (operation != StoreOperation.UPDATE)
         throw new IllegalArgumentException("Sync store operation " + operation + " not supported on client");

      assert villagers.exists(villagerInfo.getVillagerId());
      villagers.reindex(villagerInfo.getVillagerId());
      PacketDistributor.sendToServer(villagerInfo.toPacket());
      NeoForge.EVENT_BUS.post(new VillagerInfoUpdatedEvent(villagerInfo, true));
   }

   public static void receiveSyncFromServer(VillagerInfo.Packet packet, IPayloadContext context) {

      VillagerInfo fromPacket = packet.villager();

      UUID key = packet.villager().getVillagerId();
      Optional<VillagerInfo> existing = INSTANCE.find(key);

      if (packet.storeOperation() == StoreOperation.ADD_OR_OVERWRITE
            || packet.storeOperation() == StoreOperation.INIT_NEW_CLIENT) {

         if (existing.isEmpty()) {
            INSTANCE.villagers.add(fromPacket);
         } else {
            existing.get().copyFrom(fromPacket);
            INSTANCE.villagers.reindex(key);
         }

         NeoForge.EVENT_BUS.post(new VillagerInfoUpdatedEvent(existing.orElse(fromPacket), true));
         return;
      }

      if ((packet.storeOperation() == StoreOperation.UPDATE || packet.storeOperation() == StoreOperation.DELETE)
            && existing.isEmpty()) {
         LOGGER.error(
               "Server tried to sync {} storeOperation for villager {} that did not exist on {}'s client. This sync will be ignored.",
               packet.storeOperation(),
               packet.villager().toStringLite(),
               context.player().getScoreboardName());
         return;
      }

      if (packet.storeOperation() == StoreOperation.DELETE) {
         INSTANCE.villagers.remove(existing.get().getVillagerId());
      } else if (packet.storeOperation() == StoreOperation.UPDATE) {
         existing.get().copyFrom(packet.villager());
         INSTANCE.villagers.reindex(key);
      }

      NeoForge.EVENT_BUS.post(new VillagerInfoUpdatedEvent(existing.orElse(fromPacket), true));
   }

   public VillagerInfo addFromServer(VillagerInfo info) {
      Optional<VillagerInfo> existing = find(info.getVillagerId());
      if (existing.isPresent()) {
         existing.get().copyFrom(info);
         villagers.reindex(info.getVillagerId());
         return existing.get();
      }
      villagers.add(info);
      return info;
   }
}
