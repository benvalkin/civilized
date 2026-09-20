package com.uncreated.civilized.core.building;

import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.events.model.BuildingDeletedEvent;
import com.uncreated.civilized.core.building.events.model.BuildingUpdatedEvent;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientBuildingStore extends BuildingStore {

   // set the instance as early as possible because the server could sync changes when the client joins
   public static ClientBuildingStore INSTANCE = new ClientBuildingStore();

   private ClientBuildingStore() {
      super();
   }

   @Override
   public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
      throw new UnsupportedOperationException("Saving not supported on client.");
   }

   public void syncToServer() {
      for (Building building : buildings.all()) {
         PacketDistributor.sendToServer(building.toPacket());
      }
   }

   public void replicateChange(Building building, StoreOperation operation) {
      if (operation != StoreOperation.UPDATE && operation != StoreOperation.DELETE)
         throw new IllegalArgumentException("Sync store operation " + operation + " not supported on client");

      assert buildings.exists(building.getBuildingId());
      PacketDistributor.sendToServer(building.toPacket(operation));
      NeoForge.EVENT_BUS.post(new BuildingUpdatedEvent(building, true));
      if (operation == StoreOperation.DELETE)
         NeoForge.EVENT_BUS.post(new BuildingDeletedEvent(building, true));
   }

   public static void receiveSyncFromServer(Building.Packet packet, IPayloadContext context) {

      Building fromPacket = packet.building();

      UUID key = packet.building().getBuildingId();
      Optional<Building> existing = INSTANCE.find(key);

      if (packet.storeOperation() == StoreOperation.ADD_OR_OVERWRITE
            || packet.storeOperation() == StoreOperation.INIT_NEW_CLIENT) {

         if (existing.isEmpty()) {
            INSTANCE.buildings.add(fromPacket);
         } else
            existing.get().copyFrom(fromPacket);

         NeoForge.EVENT_BUS.post(new BuildingUpdatedEvent(existing.orElse(fromPacket), true));
         return;
      }

      if ((packet.storeOperation() == StoreOperation.UPDATE || packet.storeOperation() == StoreOperation.DELETE)
            && existing.isEmpty()) {
         LOGGER.error(
               "Server tried to sync {} store operation for building {} that did not exist on {}'s client. This sync will be ignored.",
               packet.storeOperation(),
               packet.building(),
               context.player().getScoreboardName());
         return;
      }

      if (packet.storeOperation() == StoreOperation.DELETE) {
         INSTANCE.buildings.remove(existing.get().getBuildingId());
      } else if (packet.storeOperation() == StoreOperation.UPDATE) {
         existing.get().copyFrom(packet.building());
      }

      NeoForge.EVENT_BUS.post(new BuildingUpdatedEvent(existing.orElse(fromPacket), true));
      if (packet.storeOperation() == StoreOperation.DELETE)
         NeoForge.EVENT_BUS.post(new BuildingUpdatedEvent(existing.orElse(fromPacket), true));
   }
}
