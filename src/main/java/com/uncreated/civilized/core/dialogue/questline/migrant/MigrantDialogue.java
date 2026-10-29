package com.uncreated.civilized.core.dialogue.questline.migrant;

import static com.uncreated.civilized.core.dialogue.DialogueScript.script;

import com.uncreated.civilized.core.dialogue.DialogueScript;

public final class MigrantDialogue {

   public static final DialogueScript SCRIPT =
         script("dialogue/migrant")
               .node("opening").sayDeterministicPerVillager("grunt")
               .choice("accept").when(new CanJoinSettlementCheck()).run(new JoinSettlementAction()).then("joined")
               .choice("reject")
               .node("joined").say("joined")
               .build();

   private MigrantDialogue() {
   }
}
