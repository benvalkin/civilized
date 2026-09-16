package com.uncreated.civilized.core.settlement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.compress.utils.Lists;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.settlement.events.SettlementUpdatedEvent;

import net.minecraft.core.BlockPos;
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

public class ServerSettlementsStore extends SettlementsStore {

   public static ServerSettlementsStore INSTANCE;

   public static void loadServer(MinecraftServer server) {
      INSTANCE =
            server.overworld()
                  .getDataStorage()
                  .computeIfAbsent(
                        new SavedData.Factory<>(ServerSettlementsStore::createDefault, ServerSettlementsStore::load),
                        STORAGE_FILE_NAME);
   }

   public static final String STORAGE_FILE_NAME = "civilized_settlements";

   // Create new instance of saved data
   private static ServerSettlementsStore createDefault() {
      return new ServerSettlementsStore();
   }

   @Override
   public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {

      ListTag tags = new ListTag();
      for (Settlement settlement : settlements.all()) {
         CompoundTag item = new CompoundTag();
         item.putUUID(Settlement.FIELD_SETTLEMENT_ID, settlement.getSettlementId());
         item.putUUID(Settlement.FIELD_OWNER_ID, settlement.getOwnerId());
         item.putString(Settlement.FIELD_DISPLAY_NAME, settlement.getDisplayName());
         item.putInt(Settlement.FIELD_SETTLEMENT_LEVEL, settlement.settlementLevel.getLevel());
         item.putLong(Settlement.FIELD_ORIGIN_POS, settlement.getBounds().getOrigin().asLong());
         item.putLong(Settlement.FIELD_LOWER_CORNER_POS, settlement.getBounds().getLowerCorner().asLong());
         item.putLong(Settlement.FIELD_UPPER_CORNER_POS, settlement.getBounds().getUpperCorner().asLong());
         ListTag citizenIds = new ListTag();
         for (UUID citizenId : settlement.getCitizenIds()) {
            CompoundTag citizenTag = new CompoundTag();
            citizenTag.putUUID(Settlement.FIELD_LIST_ITEM_CITIZEN_ID, citizenId);
            citizenIds.add(citizenTag);
         }
         item.put(Settlement.FIELD_LIST_CITIZENS, citizenIds);
         tags.add(item);
      }

      tag.put(STORAGE_FILE_NAME, tags);
      return tag;
   }

   // Load existing instance of saved data
   private static ServerSettlementsStore load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
      ServerSettlementsStore store = new ServerSettlementsStore();

      ListTag list = tag.getList(STORAGE_FILE_NAME, Tag.TAG_COMPOUND);
      for (Tag t : list) {
         if (!(t instanceof CompoundTag itemTag)) {
            continue;
         }
         Settlement.SettlementBuilder builder =
               new Settlement.SettlementBuilder().settlementId(itemTag.getUUID(Settlement.FIELD_SETTLEMENT_ID))
                     .ownerId(itemTag.getUUID(Settlement.FIELD_OWNER_ID))
                     .displayName(itemTag.getString(Settlement.FIELD_DISPLAY_NAME))
                     .settlementLevel(SettlementLevel.valueOf(itemTag.getInt(Settlement.FIELD_SETTLEMENT_LEVEL)))
                     .bounds(
                           new SettlementBounds(
                                 BlockPos.of(itemTag.getLong(Settlement.FIELD_ORIGIN_POS)),
                                 BlockPos.of(itemTag.getLong(Settlement.FIELD_LOWER_CORNER_POS)),
                                 BlockPos.of(itemTag.getLong(Settlement.FIELD_UPPER_CORNER_POS))));

         ListTag citizenIdsTag = itemTag.getList(Settlement.FIELD_LIST_CITIZENS, Tag.TAG_COMPOUND);
         List<UUID> citizenIds = Lists.newArrayList();
         for (Tag c : citizenIdsTag) {
            if (!(c instanceof CompoundTag cc))
               continue;

            citizenIds.add(cc.getUUID(Settlement.FIELD_LIST_ITEM_CITIZEN_ID));
         }

         builder.citizenIds(citizenIds);
         Settlement settlement = builder.build();
         store.settlements.add(settlement);
      }

      return store;
   }

   public void replicateChange(Settlement settlement, StoreOperation operation) {
      assert settlements.exists(settlement.getSettlementId());
      PacketDistributor.sendToAllPlayers(settlement.toPacket(operation));
      NeoForge.EVENT_BUS.post(new SettlementUpdatedEvent(settlement, false));
   }

   public void replicateFullToNewClient(ServerPlayer player) {
      for (Settlement settlement : settlements.all()) {
         PacketDistributor.sendToPlayer(player, settlement.toPacket(StoreOperation.INIT_NEW_CLIENT));
      }
   }

   public static void receiveSyncFromClient(Settlement.Packet packet, IPayloadContext context) {

      Settlement fromPacket = packet.settlement();

      UUID key = fromPacket.getSettlementId();
      Optional<Settlement> existing = INSTANCE.find(key);

      if (packet.storeOperation() == StoreOperation.ADD_OR_OVERWRITE
            || packet.storeOperation() == StoreOperation.INIT_NEW_CLIENT) {

         if (existing.isEmpty())
            INSTANCE.settlements.add(fromPacket);
         else
            existing.get().copyFrom(fromPacket);

         INSTANCE.setDirty();
         INSTANCE.replicateChange(fromPacket, packet.storeOperation());
         return;
      }

      if ((packet.storeOperation() == StoreOperation.UPDATE || packet.storeOperation() == StoreOperation.DELETE)
            && existing.isEmpty()) {
         LOGGER.error(
               "Client {} tried to sync {} storeOperation for settlement {} that did not exist on the server. This sync will be ignored.",
               packet.storeOperation(),
               context.player().getScoreboardName(),
               packet.settlement().toStringLite());
         return;
      }

      if (packet.storeOperation() == StoreOperation.DELETE) {
         Optional<Settlement> removed = INSTANCE.settlements.remove(existing.get().getSettlementId());
         if (removed.isPresent()) {
            INSTANCE.replicateChange(removed.get(), StoreOperation.DELETE);
            INSTANCE.setDirty();
         }
      } else if (packet.storeOperation() == StoreOperation.UPDATE) {
         existing.get().copyFrom(packet.settlement());
         INSTANCE.replicateChange(existing.get(), StoreOperation.UPDATE);
         INSTANCE.setDirty();
      }
   }
}
