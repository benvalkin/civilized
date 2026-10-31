package com.uncreated.civilized.core.dialogue.questline.migrant;

import static com.uncreated.civilized.core.dialogue.DialogueScript.script;
import static com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions.JOIN_SETTLEMENT;
import static com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions.WELCOME;

import com.uncreated.civilized.core.dialogue.DialogueScript;

public final class MigrantDialogue {

   // @formatter:off
   public static final DialogueScript SCRIPT =
         script("dialogue/migrant")
               .node("opening").sayDeterministicPerVillager("grunt")
               .choice(WelcomeCheck::choiceText).when(new WelcomeCheck()).runOnServer(WELCOME).then("welcomed")
               .choiceAction("join").when(new CanJoinSettlementCheck()).runOnServer(JOIN_SETTLEMENT).then("joined")
               .leave()
               .node("welcomed").say("welcomed")
               .node("joined").say("joined")
               .build();
   // @formatter:on

   private MigrantDialogue() {
   }
}
