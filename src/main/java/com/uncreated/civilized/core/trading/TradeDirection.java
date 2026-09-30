package com.uncreated.civilized.core.trading;

import java.util.Locale;

import net.minecraft.network.chat.Component;

public enum TradeDirection {
   BUY,
   SELL;

   public Component translation() {
      return Component.translatable("menu.trading.direction." + name().toLowerCase(Locale.ROOT));
   }

   public Component description() {
      return Component.translatable("menu.trading.direction." + name().toLowerCase(Locale.ROOT) + ".description");
   }
}
