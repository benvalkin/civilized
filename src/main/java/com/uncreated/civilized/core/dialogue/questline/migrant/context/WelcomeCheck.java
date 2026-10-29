package com.uncreated.civilized.core.dialogue.questline.migrant.context;

import com.uncreated.civilized.core.dialogue.ResponseOption;
import com.uncreated.civilized.core.dialogue.context.ResponseOptionContext;

public class WelcomeCheck implements ResponseOption.IResponseOptionVisibleCheck {

   @Override
   public boolean isOptionVisible(ResponseOptionContext context) {
      return !context.getDialogueContext().getVillager().getInfo().getHospitality().isWelcomed();
   }
}
