package com.uncreated.civilized.core.dialogue.questline.migrant.context;

import static net.minecraft.network.chat.Component.translatable;

import com.uncreated.civilized.core.dialogue.ResponseOption;
import com.uncreated.civilized.core.dialogue.context.ResponseOptionContext;
import com.uncreated.civilized.ui.style.Colors;

public class CanJoinSettlementCheck implements ResponseOption.IResponseOptionEnabledCheck {

   @Override
   public ResponseOption.EnabledCheckResult isOptionEnabled(ResponseOptionContext context) {

      JoinSettlementContext joinSettlement = context.getDialogueContext().as();

      if (joinSettlement.getSettlement() == null) {
         return ResponseOption.EnabledCheckResult.failed(
               translatable("villager.dialogue.traveller.quest.validation.tooltip.player_no_settlement")
                     .withColor(Colors.VALIDATION_ERROR));
      }

      if (joinSettlement.getFreeWorksite() == null) {
         return ResponseOption.EnabledCheckResult.failed(
               translatable("villager.dialogue.quest.migrant_worker.misc.response.accept.tooltip.option_disabled")
                     .withColor(Colors.VALIDATION_ERROR));
      }

      return ResponseOption.EnabledCheckResult.success(
            translatable(
                  "villager.dialogue.quest.migrant_worker.generic.any_occupation.response.accept.tooltip.option_enabled")
                  .withColor(Colors.VALIDATION_SUCCESS));
   }
}
