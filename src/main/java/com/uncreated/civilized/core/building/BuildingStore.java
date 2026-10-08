package com.uncreated.civilized.core.building;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.placement.ServerPlacementChecks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public abstract class BuildingStore extends SavedData {

   protected static final Logger LOGGER = LogUtils.getLogger();

   protected BuildingDB buildings;

   protected BuildingStore() {
      buildings = new BuildingDB();
   }

   public ImmutableList<Building> all() {
      return buildings.all();
   }

   public Building createNew(
         HolderLookup.Provider registryAccess,
         ResourceKey<Level> dimension,
         UUID settlementId,
         UUID placerId,
         BuildingType buildingType,
         BuildingBounds bounds) {
      Building building =
            Building.builder()
                  .registryAccess(registryAccess)
                  .dimension(dimension)
                  .buildingId(UUID.randomUUID())
                  .settlementId(settlementId)
                  .buildingType(buildingType)
                  .placerId(placerId)
                  .bounds(bounds)
                  .build();

      buildings.add(building);
      setDirty();

      return building;
   }

   private static final int ENCLOSING_SEARCH_CHUNK_RADIUS = ServerPlacementChecks.MAX_HORIZONTAL_SIZE / 2 / 16 + 1;

   private static final int OVERLAP_SEARCH_CHUNK_RADIUS = ServerPlacementChecks.MAX_HORIZONTAL_SIZE / 16 + 1;

   public void changeBounds(Building building, BuildingBounds newBounds) {
      // add and remove from the DB to reindex it
      buildings.remove(building.getBuildingId());
      building.setBounds(newBounds);
      buildings.add(building);
      setDirty();
   }

   // BAD IMPLEMENTATION: in future we should migrate all synced DBs to have specific client update packets instead of
   // being able to write to the DB unchecked.
   /** wrapper around copyFrom that re-indexes the building in its DB. */
   protected void updateFrom(Building existing, Building from) {
      // add and remove from the DB to reindex it
      buildings.remove(existing.getBuildingId());
      existing.copyFrom(from);
      buildings.add(existing);
   }

   public Optional<Building> find(@Nullable UUID buildingId) {
      return buildings.find(buildingId);
   }

   public Set<Building> findForSettlement(@Nullable UUID settlementId) {
      return buildings.getSettlementsToBuildingsIndex().getValues(settlementId);
   }

   public Building get(@Nullable UUID buildingId) {
      return find(buildingId).orElseThrow();
   }

   public Set<Building> findInChunk(ChunkPos chunkPos, Level level) {
      return buildings.getChunkIndex()
            .getValues(chunkPos)
            .stream()
            .filter(b -> b.getDimension().equals(level.dimension()))
            .collect(Collectors.toSet());
   }

   public Optional<Building> findEnclosingBuilding(BlockPos blockPos, Level level) {

      // Instead of querying the position of every building in the BuildingStore, we only look buildings in nearby
      // chunks to the specified point.
      // This is done using the building-chunk DB store index, and should be much faster on average (max 9 chunk index
      // lookups)

      ChunkPos centerChunk = new ChunkPos(blockPos);
      Set<ChunkPos> neighborChunks =
            ChunkPos.rangeClosed(centerChunk, ENCLOSING_SEARCH_CHUNK_RADIUS).collect(Collectors.toSet());

      for (ChunkPos chunk : neighborChunks) {
         // find the first building in this chunk that contains our point
         Optional<Building> enclosing =
               buildings.getChunkIndex()
                     .getValues(chunk)
                     .stream()
                     .filter(b -> b.getDimension().equals(level.dimension()) && b.getBounds().contains(blockPos))
                     .findFirst();

         // if it exists, return it
         if (enclosing.isPresent())
            return enclosing;
      }

      return Optional.empty();
   }

   public Optional<Building> findOverlappingBuilding(BuildingBounds bounds, Level level) {

      // Instead of querying the position of every building in the BuildingStore, we only look buildings in nearby
      // chunks to the specified point.
      // This is done using the building-chunk DB store index, and should be much faster on average (max 9 chunk index
      // lookups)

      ChunkPos centerChunk = new ChunkPos(bounds.getCenter());
      Set<ChunkPos> neighborChunks =
            ChunkPos.rangeClosed(centerChunk, OVERLAP_SEARCH_CHUNK_RADIUS).collect(Collectors.toSet());

      for (ChunkPos chunk : neighborChunks) {
         // find the first building in this chunk that overlaps our bounds
         Optional<Building> overlapping =
               buildings.getChunkIndex()
                     .getValues(chunk)
                     .stream()
                     .filter(b -> b.getDimension().equals(level.dimension()) && b.getBounds().isOverlapping(bounds))
                     .findFirst();

         // if it exists, return it
         if (overlapping.isPresent())
            return overlapping;
      }

      return Optional.empty();
   }

   public Optional<Building> findStorehouse(UUID settlementId) {
      return findForSettlement(settlementId).stream()
            .filter(b -> b.getBuildingType() == BuildingTypes.STOREHOUSE)
            .findFirst();
   }

   public Optional<Building> findTownSquare(UUID settlementId) {
      return findForSettlement(settlementId).stream()
            .filter(b -> b.getBuildingType() == BuildingTypes.TOWN_SQUARE)
            .findFirst();
   }

   public Optional<Building> findTownHall(UUID settlementId) {
      return findForSettlement(settlementId).stream()
            .filter(b -> b.getBuildingType() == BuildingTypes.TOWN_HALL)
            .findFirst();
   }
}
