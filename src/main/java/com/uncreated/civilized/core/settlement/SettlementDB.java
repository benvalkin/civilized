package com.uncreated.civilized.core.settlement;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.InMemoryDB;
import com.uncreated.civilized.core.SetIndex;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class SettlementDB extends InMemoryDB<UUID, Settlement> {

   private record DimensionChunk(ResourceKey<Level> dimension, ChunkPos chunk) {
   }

   private final SetIndex<DimensionChunk, Settlement> chunkIndex = new SetIndex<>();

   /**
    * This reverse mapping is necessary for unindexing/reindexing. To remove or reindex a settlement, you have to know
    * which chunks it's currently filed under. It is not safe to use a {@link Settlement}'s bounds to remove old indexed
    * chunks. If the {@link Settlement} instance's bounds were recalculated and no longer cover certain chunks that are
    * still stored in the index that now need to be removed, you have nothing that can be used identify and remove these
    * no-longer-covered chunks. This is very likely given that we commonly update the Settlement DB after recalculating
    * a settlement's bounds.
    */
   private final Map<Settlement, List<DimensionChunk>> currentlyIndexedChunks = new IdentityHashMap<>();

   @Override
   protected UUID getKey(Settlement obj) {
      return obj.getSettlementId();
   }

   @Override
   protected void index(Settlement obj) {
      List<DimensionChunk> chunks = coveredChunks(obj);
      for (DimensionChunk chunk : chunks)
         chunkIndex.add(chunk, obj);
      currentlyIndexedChunks.put(obj, chunks);
   }

   @Override
   protected void unindex(Settlement obj) {
      List<DimensionChunk> chunks = currentlyIndexedChunks.remove(obj);
      if (chunks == null)
         return;

      for (DimensionChunk chunk : chunks)
         chunkIndex.remove(chunk, obj);
   }

   private static List<DimensionChunk> coveredChunks(Settlement settlement) {
      SettlementBounds bounds = settlement.getBounds();
      int minChunkX = SectionPos.blockToSectionCoord(bounds.getLowerCorner().getX());
      int maxChunkX = SectionPos.blockToSectionCoord(bounds.getUpperCorner().getX());
      int minChunkZ = SectionPos.blockToSectionCoord(bounds.getLowerCorner().getZ());
      int maxChunkZ = SectionPos.blockToSectionCoord(bounds.getUpperCorner().getZ());

      List<DimensionChunk> chunks = new ArrayList<>();
      for (int x = minChunkX; x <= maxChunkX; x++) {
         for (int z = minChunkZ; z <= maxChunkZ; z++)
            chunks.add(new DimensionChunk(settlement.getDimension(), new ChunkPos(x, z)));
      }
      return chunks;
   }

   public Optional<Settlement> findEnclosing(BlockPos pos, ResourceKey<Level> dimension) {
      return chunkIndex.getValues(new DimensionChunk(dimension, new ChunkPos(pos)))
            .stream()
            .filter(settlement -> settlement.getBounds().contains(pos))
            .findFirst();
   }
}
