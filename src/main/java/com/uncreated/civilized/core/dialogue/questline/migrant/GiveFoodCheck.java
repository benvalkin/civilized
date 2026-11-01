package com.uncreated.civilized.core.dialogue.questline.migrant;

import static com.uncreated.civilized.core.dialogue.DialogueLines.text;
import static net.minecraft.network.chat.Component.translatable;

import com.uncreated.civilized.core.dialogue.ChoiceCondition;
import com.uncreated.civilized.core.dialogue.ChoiceState;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.recruitment.VisitorHospitality;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public class GiveFoodCheck implements ChoiceCondition {

   public final int minimumFood;

   public GiveFoodCheck(int minimumFood) {
      this.minimumFood = minimumFood;
   }

   @Override
   public ChoiceState check(DialogueContext context) {
      // only visitors are limited to one meal
      VisitorHospitality hospitality = context.getVisitorHospitality();
      if (hospitality != null && hospitality.isFed())
         return ChoiceState.disabled(text("recruitment/tooltips", "already_fed"));

      ItemStack held = context.heldItem();
      FoodProperties foodProperties = held.get(DataComponents.FOOD);

      if (held.isEmpty() || foodProperties == null)
         return ChoiceState.disabled(translatable("villager.dialogue.misc.offer_food.tooltip.offer_food"));

      if (foodProperties.nutrition() < minimumFood)
         return ChoiceState.disabled(translatable("villager.dialogue.misc.offer_food.tooltip.insubstantial_meal"));

      return ChoiceState.enabled();
   }
}
