package com.uncreated.civilized.core.dialogue.questline.migrant;

import static com.uncreated.civilized.core.dialogue.DialogueScript.script;
import static com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions.*;
import static net.minecraft.network.chat.Component.translatable;

import com.uncreated.civilized.core.dialogue.DialogueScript;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.network.chat.Component;

public final class VagrantDialogue {

   private static final Component PLUS_IMPRESSION =
         translatable("villager.dialogue.misc.tooltip.plus_impression", 5).withColor(Colors.VALIDATION_SUCCESS);

   // @formatter:off
   public static final DialogueScript SCRIPT =
         script("dialogue/vagrant").linesFileName("grunt")
               .node("opening").say("grunt")
               .choice(WelcomeCheck::choiceText).when(new WelcomeCheck()).runOnServer(WELCOME).then("welcomed")
               .choiceAction("offer_food").when(new GiveFoodCheck(MINIMUM_FOOD_TO_GIVE)).tooltip(PLUS_IMPRESSION)
               .runOnServer(GIVE_FOOD).then("fed")
               .choiceAction("join").when(new CanJoinSettlementCheck()).runOnServer(JOIN_SETTLEMENT).then("joined")
               .leave()
               .node("welcomed").say("welcomed")
               .node("fed").say("fed").choicesFromOtherNode("opening")
               .node("joined").say("joined")
               .build();
   // @formatter:on

   private VagrantDialogue() {
   }
}
