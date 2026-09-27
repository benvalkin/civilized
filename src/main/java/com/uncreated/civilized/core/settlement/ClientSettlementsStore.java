package com.uncreated.civilized.core.settlement;

import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.settlement.events.SettlementUpdatedEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientSettlementsStore extends SettlementsStore {

   // set the instance as early as possible because the server could sync changes when the client joins
   public static ClientSettlementsStore INSTANCE = new ClientSettlementsStore();

   private ClientSettlementsStore() {
      super();
   }

   @Override
   public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
      throw new UnsupportedOperationException("Saving not supported on client.");
   }

   public void replicateChange(Settlement settlement, StoreOperation operation) {
      if (operation != StoreOperation.UPDATE)
         throw new IllegalArgumentException("Sync store operation " + operation + " not supported on client");

      assert settlements.exists(settlement.getSettlementId());
      PacketDistributor.sendToServer(settlement.toPacket());
      NeoForge.EVENT_BUS.post(new SettlementUpdatedEvent(settlement, true));
   }

   public static void receiveSyncFromServer(Settlement.Packet packet, IPayloadContext context) {

      Settlement fromPacket = packet.settlement();

      UUID key = packet.settlement().getSettlementId();
      Optional<Settlement> existing = INSTANCE.find(key);

      if (packet.storeOperation() == StoreOperation.ADD_OR_OVERWRITE
            || packet.storeOperation() == StoreOperation.INIT_NEW_CLIENT) {

         if (existing.isEmpty()) {
            INSTANCE.settlements.add(fromPacket);
         } else {
            existing.get().copyFrom(fromPacket);
            INSTANCE.settlements.reindex(key);
         }

         NeoForge.EVENT_BUS.post(new SettlementUpdatedEvent(existing.orElse(fromPacket), true));
         return;
      }

      if ((packet.storeOperation() == StoreOperation.UPDATE || packet.storeOperation() == StoreOperation.DELETE)
            && existing.isEmpty()) {
         LOGGER.error(
               "Server tried to sync {} store operation for settlement {} that did not exist on {}'s client. This sync will be ignored.",
               packet.storeOperation(),
               packet.settlement().toStringLite(),
               context.player().getScoreboardName());
         return;
      }

      if (packet.storeOperation() == StoreOperation.DELETE) {
         INSTANCE.settlements.remove(existing.get().getSettlementId());
      } else if (packet.storeOperation() == StoreOperation.UPDATE) {
         existing.get().copyFrom(packet.settlement());
         // its bounds may have changed, which changes the chunks it's indexed under
         INSTANCE.settlements.reindex(key);
      }

      NeoForge.EVENT_BUS.post(new SettlementUpdatedEvent(existing.orElse(fromPacket), true));
   }
}
