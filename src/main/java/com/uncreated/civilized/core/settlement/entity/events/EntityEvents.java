package com.uncreated.civilized.core.settlement.entity.events;

import java.util.Set;
import java.util.function.Consumer;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.events.model.BuildingDeletedEvent;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;
import com.uncreated.civilized.core.settlement.entity.LoadedVillagers;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber
public class EntityEvents {

   @SubscribeEvent
   public static void serverPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
      if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer serverPlayer)
         ServerBuildingsStore.INSTANCE.replicateFullToNewClient(serverPlayer);
   }

   @SubscribeEvent
   public static void onServerTick(final ServerTickEvent.Post event) {
      LoadedBuildings.tickLoadedBuildings();
      LoadedSettlements.tickLoadedSettlements();
   }

   @SubscribeEvent
   public static void onBuildingDeleted(BuildingDeletedEvent event) {
      if (event.isClientside())
         return;

      LoadedBuildings.checkLoaded(event.getBuilding()).ifPresent(LoadedSettlements::onBuildingUnloaded);
      LoadedBuildings.unload(event.getBuilding().getBuildingId());
   }

   @SubscribeEvent
   public static void onChunkLoaded(ChunkEvent.Load event) {

      if (event.getLevel().isClientSide())
         return;

      if (!(event.getLevel() instanceof ServerLevel serverLevel))
         return;

      Set<Building> toAdd = ServerBuildingsStore.INSTANCE.findInChunk(event.getChunk().getPos(), serverLevel);

      toAdd.forEach(building -> {
         LoadedBuilding loadedBuilding = LoadedBuildings.load(building, serverLevel);
         LoadedSettlements.onBuildingLoaded(
               ServerSettlementsStore.INSTANCE.get(building.getSettlementId()),
               loadedBuilding,
               serverLevel);
      });

      // chests in this chunk may belong to a building that was already loaded from one of its other chunks
      forEachChestInChunk(event.getChunk(), serverLevel, chest -> LoadedBuildings.onChestLoaded(chest, serverLevel));
   }

   @SubscribeEvent
   public static void onChunkUnloaded(ChunkEvent.Unload event) {

      if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel serverLevel))
         return;

      forEachChestInChunk(event.getChunk(), serverLevel, chest -> LoadedBuildings.onChestUnloaded(chest, serverLevel));

      Set<Building> toRemove = ServerBuildingsStore.INSTANCE.findInChunk(event.getChunk().getPos(), serverLevel);

      toRemove.forEach(building -> {
         LoadedBuildings.checkLoaded(building).ifPresent(LoadedSettlements::onBuildingUnloaded);
         LoadedBuildings.unload(building.getBuildingId());
      });
   }

   private static void forEachChestInChunk(
         ChunkAccess chunk,
         ServerLevel serverLevel,
         Consumer<ChestBlockEntity> action) {

      for (BlockPos blockPos : chunk.getBlockEntitiesPos()) {
         if (serverLevel.getBlockEntity(blockPos) instanceof ChestBlockEntity chest)
            action.accept(chest);
      }
   }

   @SubscribeEvent
   public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {

      if (!(event.getLevel() instanceof ServerLevel serverLevel))
         return;

      if (serverLevel.getBlockEntity(event.getPos()) instanceof ChestBlockEntity chest)
         LoadedBuildings.onChestLoaded(chest, serverLevel);
   }

   @SubscribeEvent
   public static void onBlockBroken(BlockEvent.BreakEvent event) {

      if (!(event.getLevel() instanceof ServerLevel serverLevel))
         return;

      if (serverLevel.getBlockEntity(event.getPos()) instanceof ChestBlockEntity chest)
         LoadedBuildings.onChestUnloaded(chest, serverLevel);
   }

   @SubscribeEvent
   public static void onEntityJoinLevel(EntityJoinLevelEvent event) {

      if (!(event.getEntity() instanceof CivilizedVillager civilizedVillager))
         return;

      if (event.getLevel().isClientSide())
         return;

      LoadedVillagers.onVillagerJoinLevel(civilizedVillager);
   }

   @SubscribeEvent
   public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {

      if (!(event.getEntity() instanceof CivilizedVillager civilizedVillager))
         return;

      if (event.getLevel().isClientSide())
         return;

      LoadedVillagers.onVillagerLeaveLevel(civilizedVillager);
   }
}
