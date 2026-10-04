package com.uncreated.civilized.core.building.requirement;

import com.uncreated.civilized.ui.style.Colors;

import lombok.Getter;
import net.minecraft.network.chat.Component;

public class CurrencyRequirement implements IBuildingRequirement {

   @Getter
   private final int requiredCurrency;

   public CurrencyRequirement(int requiredCurrency) {
      this.requiredCurrency = requiredCurrency;
   }

   public Result getResult(int actualCurrency) {

      return new Result(actualCurrency, requiredCurrency);
   }

   public class Result implements IBuildingRequirementResult {

      private final int actualCurrency;
      @Getter
      private final int requiredCurrency;

      private Result(int requiredCurrency, int actualCurrency) {
         this.actualCurrency = actualCurrency;
         this.requiredCurrency = requiredCurrency;
      }

      @Override
      public boolean isSatisfied() {
         return actualCurrency >= requiredCurrency;
      }

      @Override
      public Component getDescription() {
         int numberToDisplay = Math.clamp(actualCurrency, 0, requiredCurrency);
         return Component
               .translatable(
                     "menu.building.management.requirements.currency.description",
                     numberToDisplay,
                     requiredCurrency)
               .withColor(Colors.MENU_TEXT_DARK);
      }

      @Override
      public Component getTooltipDescription() {
         return Component.translatable("menu.building.management.requirements.currency.tooltip", requiredCurrency);
      }
   }
}
