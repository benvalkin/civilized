package com.uncreated.civilized.core.dialogue.questline.advisor;

import static com.uncreated.civilized.core.dialogue.Dialogue.*;
import static com.uncreated.civilized.core.dialogue.DialogueWithPages.dialogueWithpages;
import static com.uncreated.civilized.core.dialogue.RandomSpeech.randomOneOf;
import static com.uncreated.civilized.core.dialogue.ResponseOption.closeDialogue;
import static com.uncreated.civilized.core.dialogue.ResponseOption.option;
import static com.uncreated.civilized.core.dialogue.actions.SoundActions.playerVillagerSound;
import static com.uncreated.civilized.core.dialogue.actions.quest.QuestActions.*;
import static com.uncreated.civilized.core.dialogue.rewards.RewardActions.rewardCurrency;
import static com.uncreated.civilized.core.dialogue.specialized.ItemDepotDialogue.itemDepotDialogue;
import static net.minecraft.network.chat.Component.translatable;

import java.util.Optional;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.dialogue.Dialogue;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.dialogue.controller.DialogueFlow;
import com.uncreated.civilized.core.dialogue.controller.MissingOpeningDialogueException;
import com.uncreated.civilized.core.dialogue.specialized.ItemDepotDialogue;
import com.uncreated.civilized.core.quest.ActivatedQuest;
import com.uncreated.civilized.core.quest.Quests;
import com.uncreated.civilized.core.quest.attachments.PlayerQuests;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public class AdvisorIntroQuest1 extends DialogueFlow {

   @Override
   protected @Nullable Dialogue getOpeningDialogue(
         DialogueContext context,
         CivilizedVillager villager,
         Player player,
         PlayerQuests playerQuests) {

      Optional<ActivatedQuest> activatedQuest =
            playerQuests.tryGetQuest(Quests.ADVISOR_INTRO_1, villager.getVillagerId());
      if (activatedQuest.isEmpty())
         return opening();

      if (activatedQuest.get().isStarted()) {
         if (activatedQuest.get().isCompleted()) {
            return questComplete();
         } else {
            if (playerIsHoldingFood(player))
               return consumeFood();

            return questCannotComplete();
         }
      }

      throw new MissingOpeningDialogueException();
   }

   private static Dialogue opening() {
      return dialogueWithpages()
            .page(
                  simplePage(
                        translatable("villager.dialogue.quest.advisor_intro_1.page_1"),
                        translatable("villager.dialogue.quest.advisor_intro_1.page_1.response")))
            .page(simplePage(translatable("villager.dialogue.quest.advisor_intro_1.page_2")))
            .page(simplePage(translatable("villager.dialogue.quest.advisor_intro_1.page_3")))
            .page(simplePage(translatable("villager.dialogue.quest.advisor_intro_1.page_4")))
            .page(
                  dialogue(translatable("villager.dialogue.quest.advisor_intro_1.page_5"))
                        .response(
                              option(translatable("villager.dialogue.quest.advisor_intro_1.page_5.accept"))
                                    .onSelect(startQuest(Quests.ADVISOR_INTRO_1)))
                        .response(option(translatable("villager.dialogue.traveller.quest.stay_at_village.reject"))))
            .create();
   }

   private static Dialogue questComplete() {
      return dialogueWithpages().page(simplePage(translatable("villager.dialogue.quest.advisor_intro_1.complete.1")))
            .page(finalPage(translatable("villager.dialogue.quest.advisor_intro_1.complete.2")).onEnded(context -> {
               endQuest(context, Quests.ADVISOR_INTRO_1);
               rewardCurrency(context, 6);
            }))
            .create();
   }

   private static Dialogue questCannotComplete() {
      return dialogue(
            randomOneOf(
                  translatable("villager.dialogue.quest.advisor_intro_1.incomplete.o1"),
                  translatable("villager.dialogue.quest.advisor_intro_1.incomplete.o2"),
                  translatable("villager.dialogue.quest.advisor_intro_1.incomplete.o3"),
                  translatable("villager.dialogue.quest.advisor_intro_1.incomplete.o4")))
            .response(
                  closeDialogue().withTooltip(
                        Tooltip.create(
                              Component.translatable(
                                    "villager.dialogue.quest.advisor_intro_1.incomplete.response.disabled.tooltip"))));
   }

   private static ItemDepotDialogue consumeFood() {
      return itemDepotDialogue().consumeItemAction((context, itemStack, hand) -> {
         playerVillagerSound(context, SoundEvents.PLAYER_BURP);
         completeQuest(context, Quests.ADVISOR_INTRO_1);
         return itemStack.consumeAndReturn(1, context.getPlayer());
      });
   }

   private boolean playerIsHoldingFood(Player player) {
      ItemStack item = player.getMainHandItem();
      FoodProperties foodProperties = item.get(DataComponents.FOOD);
      return !item.isEmpty() && foodProperties != null && foodProperties.nutrition() >= 6;
   }
}
