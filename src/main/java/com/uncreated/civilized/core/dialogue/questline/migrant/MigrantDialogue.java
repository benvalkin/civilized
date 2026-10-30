package com.uncreated.civilized.core.dialogue.questline.migrant;

import static com.uncreated.civilized.core.dialogue.DialogueScript.script;
import static com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions.JOIN_SETTLEMENT;
import static com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions.WELCOME;

import com.uncreated.civilized.core.dialogue.DialogueScript;

public final class MigrantDialogue {

   public static final DialogueScript SCRIPT =
         script("dialogue/migrant")
               .node("opening").sayDeterministicPerVillager("grunt")
               .choice(WelcomeCheck::choiceText).when(new WelcomeCheck()).runOnServer(WELCOME).then("welcomed")
               .choice("accept").when(new CanJoinSettlementCheck()).runOnServer(JOIN_SETTLEMENT).then("joined")
               .choice("reject")
               .node("welcomed").say("welcomed").choicesFromOtherNode("opening")
               .node("joined").say("joined")
               .build();

   private MigrantDialogue() {
   }
}
