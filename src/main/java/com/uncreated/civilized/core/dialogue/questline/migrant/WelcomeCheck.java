package com.uncreated.civilized.core.dialogue.questline.migrant;

import static com.uncreated.civilized.core.dialogue.DialogueLines.text;

import java.util.Optional;

import com.uncreated.civilized.core.dialogue.ChoiceCondition;
import com.uncreated.civilized.core.dialogue.ChoiceState;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.recruitment.VisitorHospitality;
import com.uncreated.civilized.core.settlement.ClientSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.network.chat.Component;

public class WelcomeCheck implements ChoiceCondition {

   @Override
   public ChoiceState check(DialogueContext context) {
      if (findVisitedSettlement(context).isEmpty() || context.getVillager().getInfo().getHospitality().isWelcomed())
         return ChoiceState.hidden();

      return ChoiceState.enabled(
            text("recruitment/tooltips", "welcome", VisitorHospitality.WELCOME_BONUS)
                  .withColor(Colors.VALIDATION_SUCCESS));
   }

   public static Component choiceText(DialogueContext context) {
      Component settlementName =
            findVisitedSettlement(context).map(Settlement::displayNameTranslation).orElse(Component.empty());
      return text("recruitment/response_options", "welcome", settlementName);
   }

   private static Optional<Settlement> findVisitedSettlement(DialogueContext context) {
      Optional<Settlement> settlement =
            ClientSettlementsStore.INSTANCE.find(context.getVillager().getInfo().getVisitingSettlementId());
      if (settlement.isEmpty())
         return Optional.empty();

      SettlementPermissions permissions =
            ServerSettlementPermissionStore.INSTANCE.getOrCreate(settlement.get().getSettlementId());
      if (!permissions.hasCitizenshipPermission(context.getPlayer().getUUID()))
         return Optional.empty();

      return settlement;
   }
}
