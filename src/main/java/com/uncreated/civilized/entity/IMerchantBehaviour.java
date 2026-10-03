package com.uncreated.civilized.entity;

import java.util.List;

import com.uncreated.civilized.core.trading.CurrencyStock;
import com.uncreated.civilized.core.trading.TradeItem;

public interface IMerchantBehaviour {
   CurrencyStock getAvailableCurrency();
   List<TradeItem> getTradeItems();
}
