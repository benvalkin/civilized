package com.uncreated.civilized.item.quality.events;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.item.quality.ItemQuality;
import com.uncreated.civilized.item.quality.QualityEffects;
import com.uncreated.civilized.neoforge.registration.attachments.MyDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
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
      if (event.getFlags().hasShiftDown() && event.getItemStack().isDamageableItem())
         event.getToolTip().add(2, durabilityLine(event.getItemStack(), quality));
   }

   private static Component durabilityLine(ItemStack itemStack, ItemQuality quality) {
      double multiplier = QualityEffects.getDurabilityLossMultiplier(itemStack, quality);
      double value = multiplier <= 1 ? multiplier : multiplier - 1;
      String percent = Math.round(Math.abs(value) * 100) + "%";
      String key = multiplier < 1 ? "item.quality.durability.slower" : "item.quality.durability.faster";
      return Component.literal(" \u2507 ").append(Component.translatable(key, percent)).withStyle(ChatFormatting.GRAY);
   }
}
