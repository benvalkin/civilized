package com.uncreated.civilized.core.dialogue.questline.migrant;

import static com.uncreated.civilized.core.dialogue.Dialogue.dialogue;
import static com.uncreated.civilized.core.dialogue.Dialogue.finalPage;
import static com.uncreated.civilized.core.dialogue.DialogueWithPages.dialogueWithpages;
import static com.uncreated.civilized.core.dialogue.RandomSpeech.randomOneOf;
import static com.uncreated.civilized.core.dialogue.RandomSpeech.randomPerVillager;
import static com.uncreated.civilized.core.dialogue.ResponseOption.closeDialogue;
import static com.uncreated.civilized.core.dialogue.ResponseOption.option;
import static com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions.runOnServer;
import static net.minecraft.network.chat.Component.translatable;

import com.uncreated.civilized.core.dialogue.IVillageDialogue;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions;
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
      return dialogueWithpages()
            .page(
                  dialogue(
                        randomPerVillager(
                              villager,
                              translatable("villager.dialogue.quest.vagrant_1"),
                              translatable("villager.dialogue.quest.vagrant_2")))
                        .response(
                              option(translatable("villager.dialogue.quest.misc.response.offer_food"))
                                    .shouldBeEnabled(new GiveFoodCheck(4))
                                    .onSelectGoTo(
                                          runOnServer(ServerDialogueActions.GIVE_FOOD),
                                          dialogueWithpages()
                                                .page(
                                                      finalPage(
                                                            randomOneOf(
                                                                  translatable("villager.dialogue.misc.thanks.1"),

                                                                  translatable("villager.dialogue.misc.thanks.2"),

                                                                  translatable("villager.dialogue.misc.thanks.3"),

                                                                  translatable("villager.dialogue.misc.thanks.4"),

                                                                  translatable("villager.dialogue.misc.thanks.5"),
                                                                  translatable("villager.dialogue.misc.thanks.6"))))
                                                .create()))
                        .response(closeDialogue()))
            .create();
   }
}
