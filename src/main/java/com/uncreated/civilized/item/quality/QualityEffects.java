package com.uncreated.civilized.item.quality;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

public final class QualityEffects {

   private QualityEffects() {
   }

   public static double getAttackDamageBonus(ItemQuality quality) {
      return statBonus(quality);
   }

   public static double getArmorBonus(ItemQuality quality) {
      return statBonus(quality);
   }

   public static double getBlockBreakSpeedBonus(ItemQuality quality) {
      return statBonus(quality);
   }

   public static double getDurabilityLossMultiplier(ItemStack itemStack, ItemQuality quality) {
      if (itemStack.is(ItemTags.ARMOR_ENCHANTABLE))
         return getArmorDurabilityLossMultiplier(quality);

      return getToolDurabilityLossMultiplier(quality);
   }

   public static double getToolDurabilityLossMultiplier(ItemQuality quality) {
      return switch (quality) {
      case AWFUL -> 1.6;
      case POOR -> 1.25;
      case NORMAL -> 1;
      case GOOD -> 0.75;
      case EXCELLENT -> 0.4;
      case MASTERWORK -> 0.25;
      case LEGENDARY -> 0.15;
      };
   }

   public static double getArmorDurabilityLossMultiplier(ItemQuality quality) {
      return switch (quality) {
      case AWFUL -> 1.3;
      case POOR -> 1.15;
      case NORMAL -> 1;
      case GOOD -> 0.9;
      case EXCELLENT -> 0.8;
      case MASTERWORK -> 0.7;
      case LEGENDARY -> 0.5;
      };
   }

   private static double statBonus(ItemQuality quality) {
      return switch (quality) {
      case AWFUL -> -0.4;
      case POOR -> -0.2;
      case NORMAL -> 0;
      case GOOD -> 0.15;
      case EXCELLENT -> 0.3;
      case MASTERWORK -> 0.5;
      case LEGENDARY -> 0.8;
      };
   }
}
