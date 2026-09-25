package com.uncreated.civilized.core.dialogue.questline.migrant;

import static com.uncreated.civilized.core.dialogue.Dialogue.dialogue;
import static com.uncreated.civilized.core.dialogue.Dialogue.finalPage;
import static com.uncreated.civilized.core.dialogue.DialogueWithPages.dialogueWithpages;
import static com.uncreated.civilized.core.dialogue.RandomSpeech.randomOneOf;
import static com.uncreated.civilized.core.dialogue.RandomSpeech.randomPerVillager;
import static com.uncreated.civilized.core.dialogue.ResponseOption.option;
import static net.minecraft.network.chat.Component.translatable;

import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.dialogue.IVillageDialogue;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.dialogue.controller.DialogueFlow;
import com.uncreated.civilized.core.dialogue.questline.migrant.context.CanJoinSettlementCheck;
import com.uncreated.civilized.core.dialogue.questline.migrant.context.JoinSettlementAction;
import com.uncreated.civilized.core.dialogue.questline.migrant.context.JoinSettlementContext;
import com.uncreated.civilized.core.quest.attachments.PlayerQuests;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public class MigrantGenericQuest extends DialogueFlow {

   @Override
   public DialogueContext buildDialogueContext(CivilizedVillager villager, Player player, InteractionHand hand) {
      return new JoinSettlementContext(villager, player, BuildingTypes.FARMER_HOUSE);
   }

   @Override
   protected IVillageDialogue getOpeningDialogue(
         DialogueContext context,
         CivilizedVillager villager,
         Player player,
         PlayerQuests playerQuests) {
      return dialogueWithpages().page(
            dialogue(
                  randomPerVillager(
                        villager,
                        translatable("villager.dialogue.quest.migrant_worker.generic_1"),
                        translatable("villager.dialogue.quest.migrant_worker.generic_2"),
                        translatable("villager.dialogue.quest.migrant_worker.generic_3"),
                        translatable("villager.dialogue.quest.migrant_worker.generic_4"),
                        translatable("villager.dialogue.quest.migrant_worker.generic_5"),
                        translatable("villager.dialogue.quest.migrant_worker.generic_6"),
                        translatable("villager.dialogue.quest.migrant_worker.generic_7"),
                        translatable("villager.dialogue.quest.migrant_worker.generic_8"),
                        translatable("villager.dialogue.quest.migrant_worker.generic_9"),
                        translatable("villager.dialogue.quest.migrant_worker.generic_10")))
                  .response(
                        option(translatable("villager.dialogue.quest.migrant_worker.any.response.accept"))
                              .shouldBeEnabled(new CanJoinSettlementCheck())
                              .onSelectGoTo(
                                    dialogueWithpages().page(
                                          finalPage(
                                                randomOneOf(
                                                      translatable(
                                                            "villager.dialogue.quest.migrant_worker.any.accepted.1"),
                                                      translatable(
                                                            "villager.dialogue.quest.migrant_worker.any.accepted.2"),
                                                      translatable(
                                                            "villager.dialogue.quest.migrant_worker.any.accepted.3")))
                                                .onEnded(new JoinSettlementAction()))
                                          .create()))
                  .response(option(translatable("villager.dialogue.quest.migrant_worker.any.response.reject"))))
            .create();
   }
}
