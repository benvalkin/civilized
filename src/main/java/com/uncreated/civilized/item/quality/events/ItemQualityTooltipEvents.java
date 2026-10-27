package com.uncreated.civilized.item.quality.events;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.item.quality.ItemQuality;
import com.uncreated.civilized.item.quality.QualityEffects;
import com.uncreated.civilized.neoforge.registration.attachments.MyDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = CivilizedMod.CIVILIZED_MOD_ID)
public class ItemQualityTooltipEvents {

   @SubscribeEvent
   public static void onItemTooltip(ItemTooltipEvent event) {
      ItemQuality quality = event.getItemStack().get(MyDataComponents.ITEM_QUALITY);
      if (quality == null || event.getToolTip().isEmpty() || quality == ItemQuality.NORMAL)
         return;

      event.getToolTip().add(1, quality.tooltipLine());

      // holding shift shows extra detailed tooltip stats
      if (!event.getFlags().hasShiftDown())
         return;

      ItemStack itemStack = event.getItemStack();
      int line = 2;
      if (itemStack.isDamageableItem())
         event.getToolTip().add(line++, durabilityLine(itemStack, quality));

      if (itemStack.getItem() instanceof ProjectileWeaponItem)
         event.getToolTip().add(line, projectileDamageLine(quality));
   }

   private static Component projectileDamageLine(ItemQuality quality) {
      long percent = Math.round(QualityEffects.getProjectileDamageBonus(quality) * 100);
      String signedPercent = (percent > 0 ? "+" : "") + percent + "%";
      return Component.literal(" \u2507 ")
            .append(Component.translatable("item.quality.projectile_damage", signedPercent))
            .withStyle(ChatFormatting.GRAY);
   }

   private static Component durabilityLine(ItemStack itemStack, ItemQuality quality) {
      double multiplier = QualityEffects.getDurabilityLossMultiplier(itemStack, quality);
      double value = multiplier <= 1 ? multiplier : multiplier - 1;
      String percent = Math.round(Math.abs(value) * 100) + "%";
      String key = multiplier < 1 ? "item.quality.durability.slower" : "item.quality.durability.faster";
      return Component.literal(" \u2507 ").append(Component.translatable(key, percent)).withStyle(ChatFormatting.GRAY);
   }
}
