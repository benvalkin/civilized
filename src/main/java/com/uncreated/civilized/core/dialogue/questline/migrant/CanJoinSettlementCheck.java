package com.uncreated.civilized.core.dialogue.questline.migrant;

import static net.minecraft.network.chat.Component.translatable;

import java.util.Optional;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.dialogue.ChoiceCondition;
import com.uncreated.civilized.core.dialogue.ChoiceState;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.ui.style.Colors;

public class CanJoinSettlementCheck implements ChoiceCondition {

   @Override
   public ChoiceState check(DialogueContext context) {
      Optional<Settlement> settlement = JoinSettlementAction.findPlayersSettlement(context);
      if (settlement.isEmpty()) {
         return ChoiceState.disabled(
               translatable("villager.dialogue.traveller.quest.validation.tooltip.player_no_settlement")
                     .withColor(Colors.VALIDATION_ERROR));
      }

      Optional<Building> worksite = JoinSettlementAction.findFreeWorksite(context, settlement.get());
      if (worksite.isEmpty()) {
         return ChoiceState.disabled(
               translatable("villager.dialogue.quest.migrant_worker.misc.response.accept.tooltip.option_disabled")
                     .withColor(Colors.VALIDATION_ERROR));
      }

      return ChoiceState.enabled(
            translatable(
                  "villager.dialogue.quest.migrant_worker.generic.any_occupation.response.accept.tooltip.option_enabled")
                  .withColor(Colors.VALIDATION_SUCCESS));
   }
}
