package com.uncreated.civilized.core.settlement.permission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.settlement.permission.events.SettlementPermissionsUpdatedEvent;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientSettlementPermissionStore extends SettlementPermissionStore {

   // set the instance as early as possible because the server could sync changes when the client joins
   public static ClientSettlementPermissionStore INSTANCE = new ClientSettlementPermissionStore();

   private ClientSettlementPermissionStore() {
   }

   public static void resetClient() {
      INSTANCE = new ClientSettlementPermissionStore();
   }

   public SettlementPermissions getOrCreate(UUID settlementId) {
      Optional<SettlementPermissions> existing = find(settlementId);
      if (existing.isPresent())
         return existing.get();

      SettlementPermissions created = new SettlementPermissions(settlementId, List.of());
      permissions.add(created);
      return created;
   }

   @Override
   public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
      throw new UnsupportedOperationException("Saving not supported on client.");
   }

   public static void receiveSyncFromServer(SettlementPermissions.Packet packet, IPayloadContext context) {
      SettlementPermissions fromPacket = packet.permissions();
      Optional<SettlementPermissions> existing = INSTANCE.find(fromPacket.settlementId());

      switch (packet.storeOperation()) {
      case DELETE -> {
         INSTANCE.permissions.remove(fromPacket.settlementId());
         NeoForge.EVENT_BUS.post(new SettlementPermissionsUpdatedEvent(fromPacket, true));
      }
      default -> {
         if (existing.isPresent())
            existing.get().copyFrom(fromPacket);
         else
            INSTANCE.permissions.add(fromPacket);
         NeoForge.EVENT_BUS.post(new SettlementPermissionsUpdatedEvent(existing.orElse(fromPacket), true));
      }
      }
   }
}
