package com.uncreated.civilized.core.dialogue.questline.migrant;

import static com.uncreated.civilized.core.dialogue.DialogueScript.script;
import static com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions.GIVE_FOOD;
import static com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions.MINIMUM_FOOD_TO_GIVE;

import com.uncreated.civilized.core.dialogue.DialogueScript;

public final class VagrantDialogue {

   public static final DialogueScript SCRIPT =
         script("dialogue/vagrant").commonFile("grunt")
               .node("opening").say("grunt")
               .choice("offer_food").when(new GiveFoodCheck(MINIMUM_FOOD_TO_GIVE)).runOnServer(GIVE_FOOD).then("fed")
               .leave()
               .node("fed").say("fed")
               .build();

   private VagrantDialogue() {
   }
}
