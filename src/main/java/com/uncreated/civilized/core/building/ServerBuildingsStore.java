package com.uncreated.civilized.core.building;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.events.model.BuildingDeletedEvent;
import com.uncreated.civilized.core.building.events.model.BuildingUpdatedEvent;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerBuildingsStore extends BuildingStore {

   protected static final Logger LOGGER = LogUtils.getLogger();
   public static ServerBuildingsStore INSTANCE;

   protected ServerBuildingsStore() {
      super();
   }

   public static void loadServer(MinecraftServer server) {

      INSTANCE =
            server.overworld()
                  .getDataStorage()
                  .computeIfAbsent(
                        new SavedData.Factory<>(ServerBuildingsStore::createDefault, ServerBuildingsStore::load),
                        STORAGE_FILE_NAME);
   }

   public static final String STORAGE_FILE_NAME = "civilized_buildings";

   // Create a new instance of saved data
   private static ServerBuildingsStore createDefault() {
      return new ServerBuildingsStore();
   }

   @Override
   public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {

      ListTag tags = new ListTag();
      for (Building building : buildings.all()) {
         CompoundTag item = new CompoundTag();
         item.putString(Building.FIELD_DIMENSION, building.getDimension().location().toString());
         item.putUUID(Building.FIELD_BUILDING_ID, building.getBuildingId());
         item.putUUID(Building.FIELD_SETTLEMENT_ID, building.getSettlementId());
         item.putUUID(Building.FIELD_PLACER_ID, building.getPlacerId());
         item.putString(Building.FIELD_BUILDING_TYPE, building.getBuildingType().resourceLocation().toString());
         item.putLong(Building.FIELD_CENTER_POS, building.getBounds().getCenter().asLong());
         item.putLong(Building.FIELD_LOWER_CORNER_POS, building.getBounds().getLowerCorner().asLong());
         item.putLong(Building.FIELD_UPPER_CORNER_POS, building.getBounds().getUpperCorner().asLong());
         item.putInt(Building.FIELD_UPGRADE_LEVEL, building.getUpgradeLevel());

         ListTag occupantIds = new ListTag();
         for (UUID occupantId : building.getOccupantIds()) {
            CompoundTag occupantTag = new CompoundTag();
            occupantTag.putUUID(Building.FIELD_LIST_ITEM_OCCUPANT_ID, occupantId);
            occupantIds.add(occupantTag);
         }
         item.put(Building.FIELD_LIST_OCCUPANTS, occupantIds);
         item.put(Building.FIELD_BEHAVIOUR_DATA, building.getState().toNbt(registries));
         tags.add(item);
      }

      tag.put(STORAGE_FILE_NAME, tags);
      return tag;
   }

   // Load existing instance of saved data
   private static ServerBuildingsStore load(CompoundTag parentListTag, HolderLookup.Provider lookupProvider) {
      ServerBuildingsStore store = new ServerBuildingsStore();

      ListTag list = parentListTag.getList(STORAGE_FILE_NAME, Tag.TAG_COMPOUND);
      for (Tag t : list) {
         if (!(t instanceof CompoundTag itemTag))
            continue;

         Building.BuildingBuilder builder =
               Building.builder()
                     .registryAccess(lookupProvider)
                     .dimension(
                           ResourceKey.create(
                                 Registries.DIMENSION,
                                 ResourceLocation.parse(itemTag.getString(Building.FIELD_DIMENSION))))
                     .buildingId(itemTag.getUUID(Building.FIELD_BUILDING_ID))
                     .settlementId(itemTag.getUUID(Building.FIELD_SETTLEMENT_ID))
                     .placerId(itemTag.getUUID(Building.FIELD_PLACER_ID))
                     .buildingType(
                           BuildingTypes.getFromResourceLocation(
                                 ResourceLocation.parse(itemTag.getString(Building.FIELD_BUILDING_TYPE))))
                     .bounds(
                           new BuildingBounds(
                                 BlockPos.of(itemTag.getLong(Building.FIELD_CENTER_POS)),
                                 BlockPos.of(itemTag.getLong(Building.FIELD_LOWER_CORNER_POS)),
                                 BlockPos.of(itemTag.getLong(Building.FIELD_UPPER_CORNER_POS))));

         ListTag occupantIdsTag = itemTag.getList(Building.FIELD_LIST_OCCUPANTS, Tag.TAG_COMPOUND);
         List<UUID> occupantIds = new ArrayList<>();
         for (Tag o : occupantIdsTag) {
            if (!(o instanceof CompoundTag co))
               continue;

            occupantIds.add(co.getUUID(Building.FIELD_LIST_ITEM_OCCUPANT_ID));
         }

         builder.occupantIds(occupantIds);
         // buildings saved before upgrade levels existed are at the first level
         if (itemTag.contains(Building.FIELD_UPGRADE_LEVEL))
            builder.upgradeLevel(itemTag.getInt(Building.FIELD_UPGRADE_LEVEL));
         Building building = builder.build();
         building.getState().applyNbt(itemTag.getCompound(Building.FIELD_BEHAVIOUR_DATA), lookupProvider);
         store.buildings.add(building);
      }

      return store;
   }

   public void replicateChange(Building building, StoreOperation operation) {
      assert buildings.exists(building.getBuildingId());
      PacketDistributor.sendToAllPlayers(building.toPacket(operation));

      NeoForge.EVENT_BUS.post(new BuildingUpdatedEvent(building, false));
      if (operation == StoreOperation.DELETE)
         NeoForge.EVENT_BUS.post(new BuildingDeletedEvent(building, false));
   }

   public void replicateFullToNewClient(ServerPlayer player) {
      for (Building building : buildings.all()) {
         PacketDistributor.sendToPlayer(player, building.toPacket(StoreOperation.INIT_NEW_CLIENT));
      }
   }

   private static boolean allowedToDecommission(Player player, Building building) {
      SettlementPermissions permissions =
            ServerSettlementPermissionStore.INSTANCE.getOrCreate(building.getSettlementId());
      if (permissions.hasCreateBuildingsPermission(player.getUUID()))
         return true;

      player.displayClientMessage(
            Component.translatable(
                  "message.settlement.permission.denied.delete_building",
                  building.getBuildingType().translation()),
            true);
      return false;
   }

   public static void receiveSyncFromClient(Building.Packet packet, IPayloadContext context) {

      Building fromPacket = packet.building();

      UUID key = fromPacket.getBuildingId();
      Optional<Building> existing = INSTANCE.find(key);

      // buildings are only ever created by the server, after checking they're allowed, e.g. in CreateNewBuilding
      if (packet.storeOperation() == StoreOperation.ADD_OR_OVERWRITE
            || packet.storeOperation() == StoreOperation.INIT_NEW_CLIENT) {
         LOGGER.warn(
               "Client {} tried to sync {} for building {}, which clients aren't allowed to do. This sync will be ignored.",
               context.player().getScoreboardName(),
               packet.storeOperation(),
               fromPacket);
         return;
      }

      if ((packet.storeOperation() == StoreOperation.UPDATE || packet.storeOperation() == StoreOperation.DELETE)
            && existing.isEmpty()) {
         LOGGER.error(
               "Client {} tried to sync {} storeOperation for building {} that did not exist on the server. This sync will be ignored.",
               packet.storeOperation(),
               context.player().getScoreboardName(),
               packet.building());
         return;
      }

      if (packet.storeOperation() == StoreOperation.DELETE) {
         if (!allowedToDecommission(context.player(), existing.get()))
            return;

         Optional<Building> removed = INSTANCE.buildings.remove(existing.get().getBuildingId());
         if (removed.isPresent()) {
            INSTANCE.replicateChange(removed.get(), StoreOperation.DELETE);
            INSTANCE.setDirty();
         }
      } else if (packet.storeOperation() == StoreOperation.UPDATE) {
         INSTANCE.updateFrom(existing.get(), packet.building());
         INSTANCE.replicateChange(existing.get(), StoreOperation.UPDATE);
         INSTANCE.setDirty();
      }
   }
}
