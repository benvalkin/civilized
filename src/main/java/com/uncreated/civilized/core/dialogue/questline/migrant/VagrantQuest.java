package com.uncreated.civilized.core.dialogue.questline.migrant;

import static com.uncreated.civilized.core.dialogue.Dialogue.dialogue;
import static com.uncreated.civilized.core.dialogue.Dialogue.finalPage;
import static com.uncreated.civilized.core.dialogue.DialogueLines.*;
import static com.uncreated.civilized.core.dialogue.DialogueWithPages.dialogueWithpages;
import static com.uncreated.civilized.core.dialogue.ResponseOption.closeDialogue;
import static com.uncreated.civilized.core.dialogue.ResponseOption.option;
import static com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions.runOnServer;

import com.uncreated.civilized.core.dialogue.IVillageDialogue;
import com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.dialogue.controller.DialogueFlow;
import com.uncreated.civilized.core.dialogue.questline.migrant.context.GiveFoodCheck;
import com.uncreated.civilized.core.dialogue.questline.migrant.context.ItemDepotContext;
import com.uncreated.civilized.core.quest.attachments.PlayerDialogueCooldowns;
import com.uncreated.civilized.core.quest.attachments.PlayerQuests;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public class VagrantQuest extends DialogueFlow {

   @Override
   public DialogueContext buildDialogueContext(
         CivilizedVillager villager,
         Player player,
         InteractionHand hand,
         long gameTime,
         long dayTime) {
      return new ItemDepotContext(villager, player, gameTime);
   }

   @Override
   protected IVillageDialogue getOpeningDialogue(
         DialogueContext context,
         CivilizedVillager villager,
         Player player,
         PlayerQuests playerQuests,
         PlayerDialogueCooldowns cooldowns) {
      String set = chooseSetPerVillager(villager, "dialogue/vagrant", "grunt");
      return dialogueWithpages()
            .page(
                  dialogue(randomLinePerVillager(villager, set + "/grunt"))
                        .response(
                              option(text("dialogue/vagrant/response_options", "offer_food"))
                                    .shouldBeEnabled(new GiveFoodCheck(4))
                                    .onSelectGoTo(
                                          runOnServer(ServerDialogueActions.GIVE_FOOD),
                                          dialogueWithpages().page(finalPage(randomLine(set + "/fed"))).create()))
                        .response(closeDialogue()))
            .create();
   }
}
