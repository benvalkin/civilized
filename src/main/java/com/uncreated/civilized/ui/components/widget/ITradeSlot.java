package com.uncreated.civilized.ui.components.widget;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.trading.TradeItem;

/** A slot in the trading screen's grid. */
public interface ITradeSlot {

   int getSlot();

   @Nullable
   TradeItem getTradeItem();
}
