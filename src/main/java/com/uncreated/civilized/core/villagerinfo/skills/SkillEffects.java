package com.uncreated.civilized.core.villagerinfo.skills;

import java.util.List;

import com.uncreated.civilized.entity.CivilizedVillager;

/**
 * A collection of helper methods for applying modifications to the villager's work efficiency and speed.
 * <p>
 * Work rates are usually quoted in units per work day, i.e. how many units villager could produce or harvest if they
 * did nothing else all day with no breaks or other activities. Their actual production output will usually be lower
 * than this value. The baseline rate is for Skill Level 5.
 */
public final class SkillEffects {

   private static final int BASELINE_LEVEL = 5;

   private static final int BASELINE_SAPLINGS_PLANTED_PER_DAY = 500;

   // Mining
   private static final int BASELINE_BLOCKS_MINED_PER_DAY = 128;

   // Animals
   private static final int BASELINE_FISH_CAUGHT_PER_DAY = 20;

   private SkillEffects() {
   }

   /**
    * How much faster than the baseline a villager of this level works. x0.5 at level 0, x1 at level 5 and x2 at level
    * 10. Every level is the same step up from the one before it (about 15%).
    */
   public static float levelMultiplier(int level, float multiplierPower) {
      return (float) Math.pow(multiplierPower, (level - BASELINE_LEVEL) / (double) BASELINE_LEVEL);
   }

   public static int convertToTicks(int unitsPerDay) {
      return Math.max(1, Math.round(CivilizedVillager.WORK_DAY_DURATION_TICKS / (float) unitsPerDay));
   }

   public static int getCropsHarvestedPerDay(int farmingLevel) {
      return scaled(77, farmingLevel);
   }

   public static int getCropsPlantedPerDay(int farmingLevel) {
      return scaled(308, farmingLevel);
   }

   public static int getLogsHarvestedPerDay(int farmingLevel) {
      return scaled(24, farmingLevel);
   }

   public static int getMaxAllowedCrops(int farmingLevel) {
      return fixedDistribution(List.of(14, 20, 26, 32, 40, 50, 60, 80, 90, 100, 120), farmingLevel);
   }

   public static int getSaplingsPlantedPerDay(int farmingLevel) {
      return scaled(500, farmingLevel);
   }

   public static int getAttemptedBlocksMinedPerDay(int miningLevel) {
      return scaled(128, miningLevel);
   }

   public static Double getAlterHoneyAccumulationChance(int animalsLevel) {
      return fixedDistribution(List.of(0.5, 0.5, 0.4, 0.3, 0.2, 0.1, 0d, 0.15, 0.25, 0.35, 0.45, 0.6), animalsLevel);
   }

   public static int getFishCaughtPerDay(int animalsLevel) {
      return scaled(5, animalsLevel);
   }

   private static int scaled(int baselinePerDay, int level, float multiplierPower) {
      return Math.max(1, Math.round(baselinePerDay * levelMultiplier(level, multiplierPower)));
   }

   public static int getMaxAllowedProductionBills(int craftingLevel) {
      return fixedDistribution(List.of(1, 1, 2, 2, 3, 4, 4, 5, 6, 7, 8), craftingLevel);
   }

   private static int scaled(int baselinePerDay, int level) {
      return scaled(baselinePerDay, level, 2.5f);
   }

   private static <T> T fixedDistribution(List<T> values, int level) {
      if (values.isEmpty())
         throw new IllegalArgumentException("values list cannot be empty");

      if (level >= values.size())
         return values.getLast();

      return values.get(level);
   }
}
