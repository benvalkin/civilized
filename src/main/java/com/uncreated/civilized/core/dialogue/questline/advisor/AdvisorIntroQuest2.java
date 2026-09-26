package com.uncreated.civilized.core.dialogue.questline.advisor;

import static com.uncreated.civilized.core.dialogue.Dialogue.simplePage;
import static com.uncreated.civilized.core.dialogue.DialogueWithPages.dialogueWithpages;
import static com.uncreated.civilized.core.dialogue.actions.quest.QuestActions.completeCheckpointQuest;
import static net.minecraft.network.chat.Component.translatable;

import com.uncreated.civilized.core.dialogue.Dialogue;
import com.uncreated.civilized.core.dialogue.IVillageDialogue;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.dialogue.controller.DialogueFlow;
import com.uncreated.civilized.core.quest.Quests;
import com.uncreated.civilized.core.quest.attachments.PlayerDialogueCooldowns;
import com.uncreated.civilized.core.quest.attachments.PlayerQuests;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.world.entity.player.Player;

public class AdvisorIntroQuest2 extends DialogueFlow {

   @Override
   protected IVillageDialogue getOpeningDialogue(
         DialogueContext context,
         CivilizedVillager villager,
         Player player,
         PlayerQuests playerQuests,
         PlayerDialogueCooldowns cooldowns) {

      return opening(villager);
   }

   private static Dialogue opening(CivilizedVillager villager) {
      return dialogueWithpages()
            .page(
                  simplePage(
                        translatable("villager.dialogue.quest.advisor_intro_2.page_1"),
                        translatable("villager.dialogue.quest.advisor_intro_2.page_1.response")))
            .page(
                  simplePage(
                        translatable(
                              "villager.dialogue.quest.advisor_intro_2.page_2",
                              villager.getInfo().getFullName())))
            .page(simplePage(translatable("villager.dialogue.quest.advisor_intro_2.page_3")))
            .page(simplePage(translatable("villager.dialogue.quest.advisor_intro_2.page_4")))
            .page(simplePage(translatable("villager.dialogue.quest.advisor_intro_2.page_5")))
            .page(
                  simplePage(
                        translatable("villager.dialogue.quest.advisor_intro_2.page_6"),
                        translatable("villager.dialogue.quest.advisor_intro_2.page_6.response")))
            .page(simplePage(translatable("villager.dialogue.quest.advisor_intro_2.page_7")))
            .page(simplePage(translatable("villager.dialogue.quest.advisor_intro_2.page_8")))
            .page(
                  simplePage(
                        translatable("villager.dialogue.quest.advisor_intro_2.page_9"),
                        translatable("villager.dialogue.quest.advisor_intro_2.page_9.response")))
            .page(simplePage(translatable("villager.dialogue.quest.advisor_intro_2.page_10")))
            .page(simplePage(translatable("villager.dialogue.quest.advisor_intro_2.page_11")))
            .page(simplePage(translatable("villager.dialogue.quest.advisor_intro_2.page_12")))
            .page(
                  simplePage(
                        translatable("villager.dialogue.quest.advisor_intro_2.page_13"),
                        translatable("villager.dialogue.quest.advisor_intro_2.page_13.response"))
                        .onEnded(context -> completeCheckpointQuest(context, Quests.ADVISOR_INTRO_2_CHECKPOINT)))
            .create();
   }
}
