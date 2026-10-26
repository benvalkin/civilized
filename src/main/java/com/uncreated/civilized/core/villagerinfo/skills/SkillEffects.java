package com.uncreated.civilized.core.villagerinfo.skills;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.item.quality.ItemQuality;
import com.uncreated.civilized.item.quality.QualityEffects;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A collection of helper methods for applying modifications to the villager's work efficiency and speed.
 * <p>
 * Work rates are usually quoted in units per work day, i.e. how many units villager could produce or harvest if they
 * did nothing else all day with no breaks or other activities. Their actual production output will usually be lower
 * than this value. The baseline rate is for Skill Level 5.
 */
public final class SkillEffects {

   private static final int BASELINE_SKILL_LEVEL = 5;

   /** Iron tool's mining speed which is used as the baseline. */
   private static final float BASELINE_TOOL_SPEED = 6;
   /**
    * Reduces how much tool mining speed affects villager work speed, mostly in place because tool materials affect
    * vanilla mining speed quite drastically (e.g. Wood -> x0.33, Gold -> x2)
    */
   private static final double TOOL_MINING_SPEED_EXPONENT = 0.75;

   private SkillEffects() {
   }

   /**
    * How much faster than the baseline a villager of this level works. x0.5 at level 0, x1 at level 5 and x2 at level
    * 10. Every level is the same step up from the one before it (about 15%).
    */
   public static float levelMultiplier(int level, float multiplierPower) {
      return (float) Math.pow(multiplierPower, (level - BASELINE_SKILL_LEVEL) / (double) BASELINE_SKILL_LEVEL);
   }

   public static int convertToTicks(int unitsPerDay) {
      return Math.max(1, Math.round(CivilizedVillager.WORK_DAY_DURATION_TICKS / (float) unitsPerDay));
   }

   public static int getCropsHarvestedPerDay(int farmingLevel, ItemStack hoe) {
      return withTool(scaled(77, farmingLevel), hoe, Blocks.HAY_BLOCK.defaultBlockState());
   }

   public static int getCropsPlantedPerDay(int farmingLevel) {
      return scaled(308, farmingLevel);
   }

   public static int getLogsHarvestedPerDay(int farmingLevel, ItemStack axe) {
      return withTool(scaled(24, farmingLevel), axe, Blocks.OAK_LOG.defaultBlockState());
   }

   public static int getMaxAllowedCrops(int farmingLevel) {
      return fixedDistribution(List.of(14, 20, 26, 32, 40, 50, 60, 80, 90, 100, 120), farmingLevel);
   }

   public static int getSaplingsPlantedPerDay(int farmingLevel) {
      return scaled(500, farmingLevel);
   }

   public static int getAttemptedBlocksMinedPerDay(int miningLevel, ItemStack pickaxe) {
      return withTool(scaled(128, miningLevel), pickaxe, Blocks.STONE.defaultBlockState());
   }

   public static Double getAlterHoneyAccumulationChance(int animalsLevel) {
      return fixedDistribution(List.of(-0.5, -0.4, -0.3, -0.2, -0.1, 0d, 0.15, 0.25, 0.35, 0.45, 0.6), animalsLevel);
   }

   public static int getFishCaughtPerDay(int animalsLevel, ItemStack fishingRod) {
      return withTool(scaled(5, animalsLevel), fishingRod, null);
   }

   private static int scaled(int baselinePerDay, int level, float multiplierPower) {
      return Math.max(1, Math.round(baselinePerDay * levelMultiplier(level, multiplierPower)));
   }

   public static int getMaxAllowedProductionBills(int craftingLevel) {
      return fixedDistribution(List.of(1, 1, 2, 2, 3, 4, 4, 5, 6, 7, 8), craftingLevel);
   }

   /**
    * Applies the specified tool's mining speed as a work speed multiplier. A better tool speeds increases the
    * villager's work speed on top of their skill, and a worse one slows them down. Mining speed is calculated by
    * applying the specified tool itemStack on a typical example block. If no example block is supplied, the tool's
    * basic destroy speed is used.
    * <p>
    * The calculation also takes into account Item Quality. Item quality is taken into on all tools, including those
    * that are not used to break blocks. This makes all high quality tools increase villager work speed, even fishing
    * rods and shears.
    *
    * @param typicalBlock
    *           an example of a typical block. null for tools that don't mine anything (e.g. fishing rods), in which
    *           case which only the tool's quality affect work speed.
    */
   private static int withTool(int unitsPerDay, ItemStack tool, @Nullable BlockState typicalBlock) {
      float miningSpeed = typicalBlock == null ? BASELINE_TOOL_SPEED : tool.getDestroySpeed(typicalBlock);
      double qualityBonus = QualityEffects.getBlockBreakSpeedBonus(ItemQuality.of(tool));
      double effectiveSpeed = miningSpeed * (1 + qualityBonus);
      double multiplier = Math.pow(effectiveSpeed / BASELINE_TOOL_SPEED, TOOL_MINING_SPEED_EXPONENT);
      return Math.max(1, (int) Math.round(unitsPerDay * multiplier));
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
