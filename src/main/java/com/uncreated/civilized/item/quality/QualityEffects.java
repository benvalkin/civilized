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
      case AWFUL -> 1.4;
      case POOR -> 1.2;
      case NORMAL -> 1;
      case GOOD -> 0.85;
      case EXCELLENT -> 0.7;
      case MASTERWORK -> 0.5;
      case LEGENDARY -> 0.3;
      };
   }

   public static double getArmorDurabilityLossMultiplier(ItemQuality quality) {
      return switch (quality) {
      case AWFUL -> 1.3;
      case POOR -> 1.15;
      case NORMAL -> 1;
      case GOOD -> 0.9;
      case EXCELLENT -> 0.8;
      case MASTERWORK -> 0.65;
      case LEGENDARY -> 0.5;
      };
   }

   private static double statBonus(ItemQuality quality) {
      return switch (quality) {
      case AWFUL -> -0.3;
      case POOR -> -0.1;
      case NORMAL -> 0;
      case GOOD -> 0.1;
      case EXCELLENT -> 0.2;
      case MASTERWORK -> 0.4;
      case LEGENDARY -> 0.6;
      };
   }
}
