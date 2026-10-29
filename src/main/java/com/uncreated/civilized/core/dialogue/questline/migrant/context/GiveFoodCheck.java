package com.uncreated.civilized.core.dialogue.questline.migrant.context;

import static com.uncreated.civilized.core.dialogue.DialogueLines.text;
import static net.minecraft.network.chat.Component.translatable;

import com.uncreated.civilized.core.dialogue.ResponseOption;
import com.uncreated.civilized.core.dialogue.context.ResponseOptionContext;
import com.uncreated.civilized.core.recruitment.VisitorHospitality;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public class GiveFoodCheck implements ResponseOption.IResponseOptionEnabledCheck {

   public final int minimumFood;

   public GiveFoodCheck(int minimumFood) {
      this.minimumFood = minimumFood;
   }

   @Override
   public ResponseOption.EnabledCheckResult isOptionEnabled(ResponseOptionContext context) {

      if (context.getDialogueContext().getVillager().getInfo().getHospitality().isFed())
         return ResponseOption.EnabledCheckResult.failed(text("recruitment/tooltips", "already_fed"));

      ItemDepotContext itemDepotContext = context.getDialogueContext().as();

      ItemStack held = itemDepotContext.getHeldItem();
      FoodProperties foodProperties = held.get(DataComponents.FOOD);

      if (held.isEmpty() || foodProperties == null) {
         return ResponseOption.EnabledCheckResult
               .failed(translatable("villager.dialogue.misc.offer_food.tooltip.offer_food"));
      }

      if (foodProperties.nutrition() < minimumFood) {
         return ResponseOption.EnabledCheckResult
               .failed(translatable("villager.dialogue.misc.offer_food.tooltip.insubstantial_meal"));
      }
      return ResponseOption.EnabledCheckResult.success(
            translatable("villager.dialogue.misc.tooltip.plus_impression", VisitorHospitality.FED_BONUS).withColor(Colors.VALIDATION_SUCCESS));
   }
}
