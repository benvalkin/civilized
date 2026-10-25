package com.uncreated.civilized.core.villagerinfo.skills;

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
   public static float levelMultiplier(int level) {
      return (float) Math.pow(2, (level - BASELINE_LEVEL) / (double) BASELINE_LEVEL);
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

   public static int getSaplingsPlantedPerDay(int farmingLevel) {
      return scaled(500, farmingLevel);
   }

   public static int getAttemptedBlocksMinedPerDay(int miningLevel) {
      return scaled(128, miningLevel);
   }

   public static int getFishCaughtPerDay(int animalsLevel) {
      return scaled(5, animalsLevel);
   }

   private static int scaled(int baselinePerDay, int level) {
      return Math.max(1, Math.round(baselinePerDay * levelMultiplier(level)));
   }
}
