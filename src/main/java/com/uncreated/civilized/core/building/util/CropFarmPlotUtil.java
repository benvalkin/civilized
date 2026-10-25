package com.uncreated.civilized.core.building.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.uncreated.civilized.core.building.Building;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;

public final class CropFarmPlotUtil {

   private CropFarmPlotUtil() {
   }

   /**
    * @return a list of farmland blocks within the bounds of the specified building, ordered by the ones closest to its
    *         center.
    */
   public static List<BlockPos> findPlots(Building cropFarm, Level level) {
      List<BlockPos> plots = new ArrayList<>();
      cropFarm.getBounds().traverseBlocksWithinTerminateYChecksIfCanSeeSky(pos -> {
         if (level.getBlockState(pos).getBlock() instanceof FarmBlock)
            plots.add(pos.immutable());
      }, level);

      BlockPos centre = cropFarm.getBounds().getCenter();
      // the list is ordered by position so that plots always occur in the same order each time the list is computed
      // (the farmer needs a deterministic set of plots in order to figure out which ones to replant/not replant
      Comparator<BlockPos> closestFirst = Comparator.comparingDouble(plot -> plot.distSqr(centre));
      plots.sort(closestFirst.thenComparing(Comparator.naturalOrder()));
      return plots;
   }

   /**
    * @return a deterministic list of farmland blocks (ordered by position from the cropFarm's center) that a farmer is
    *         allowed to tend to.
    */
   public static List<BlockPos> findTendedPlots(Building cropFarm, Level level, int maxPlots) {
      List<BlockPos> plots = findPlots(cropFarm, level);
      return plots.subList(0, Math.min(maxPlots, plots.size()));
   }
}
