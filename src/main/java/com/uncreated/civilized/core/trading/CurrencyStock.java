package com.uncreated.civilized.core.trading;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class CurrencyStock {
   @Getter
   @Setter
   private int availableCurrency;

   public CurrencyStock(int availableCurrency) {
      this.availableCurrency = availableCurrency;
   }
}
