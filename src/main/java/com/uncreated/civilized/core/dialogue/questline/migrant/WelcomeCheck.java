package com.uncreated.civilized.core.dialogue.questline.migrant;

import static com.uncreated.civilized.core.dialogue.DialogueLines.text;

import java.util.Optional;

import com.uncreated.civilized.core.dialogue.ChoiceCondition;
import com.uncreated.civilized.core.dialogue.ChoiceState;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.recruitment.VisitorHospitality;
import com.uncreated.civilized.core.settlement.ClientSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.network.chat.Component;

/** "Welcome to <settlement>!", once per visit, for players who have a settlement. */
public class WelcomeCheck implements ChoiceCondition {

   @Override
   public ChoiceState check(DialogueContext context) {
      if (findPlayersSettlement(context).isEmpty() || context.getVillager().getInfo().getHospitality().isWelcomed())
         return ChoiceState.hidden();

      return ChoiceState.enabled(
            text("recruitment/tooltips", "welcome", VisitorHospitality.WELCOME_BONUS)
                  .withColor(Colors.VALIDATION_SUCCESS));
   }

   public static Component choiceText(DialogueContext context) {
      Component settlementName =
            findPlayersSettlement(context).map(Settlement::displayNameTranslation).orElse(Component.empty());
      return text("recruitment/response_options", "welcome", settlementName);
   }

   static Optional<Settlement> findPlayersSettlement(DialogueContext context) {
      return ClientSettlementsStore.INSTANCE.findFromOwner(context.getPlayer().getUUID());
   }
}
