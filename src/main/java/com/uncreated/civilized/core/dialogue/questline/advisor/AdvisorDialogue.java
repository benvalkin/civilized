package com.uncreated.civilized.core.dialogue.questline.advisor;

import static com.uncreated.civilized.core.dialogue.DialogueScript.script;
import static com.uncreated.civilized.core.dialogue.actions.SoundActions.playerVillagerSound;
import static com.uncreated.civilized.core.dialogue.actions.quest.QuestActions.completeCheckpointQuest;
import static com.uncreated.civilized.core.dialogue.actions.quest.QuestActions.completeQuest;
import static com.uncreated.civilized.core.dialogue.actions.quest.QuestActions.endQuest;
import static com.uncreated.civilized.core.dialogue.actions.quest.QuestActions.startQuest;
import static com.uncreated.civilized.core.dialogue.rewards.RewardActions.rewardCurrency;
import static net.minecraft.network.chat.Component.translatable;

import java.util.Optional;

import com.uncreated.civilized.core.dialogue.DialogueScript;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.quest.ActivatedQuest;
import com.uncreated.civilized.core.quest.Quests;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public final class AdvisorDialogue {

   private static final String INTRO_1 = "villager.dialogue.quest.advisor_intro_1.";
   private static final String INTRO_2 = "villager.dialogue.quest.advisor_intro_2.";
   private static final int MINIMUM_FOOD_FOR_INTRO_1 = 6;

   public static final DialogueScript SCRIPT =
         script()
               .acceptsItem((context, offered) -> isWaitingForIntro1(context) && isMeal(offered), (context, offered) -> {
                  playerVillagerSound(context, SoundEvents.PLAYER_BURP);
                  completeQuest(context, Quests.ADVISOR_INTRO_1);
                  offered.consume(1, context.getPlayer());
               })

               .node("intro_1").startIf(context -> isOnIntro1(context) && intro1(context).isEmpty())
               .say(translatable(INTRO_1 + "page_1")).reply(translatable(INTRO_1 + "page_1.response"))
               .say(translatable(INTRO_1 + "page_2"))
               .say(translatable(INTRO_1 + "page_3"))
               .say(translatable(INTRO_1 + "page_4"))
               .say(translatable(INTRO_1 + "page_5"))
               .choice(translatable(INTRO_1 + "page_5.accept")).run(startQuest(Quests.ADVISOR_INTRO_1))
               .choice(translatable("villager.dialogue.traveller.quest.stay_at_village.reject"))

               .node("intro_1_waiting").startIf(AdvisorDialogue::isWaitingForIntro1)
               .sayOneOf(
                     translatable(INTRO_1 + "incomplete.o1"),
                     translatable(INTRO_1 + "incomplete.o2"),
                     translatable(INTRO_1 + "incomplete.o3"),
                     translatable(INTRO_1 + "incomplete.o4"))
               .leave().tooltip(translatable(INTRO_1 + "incomplete.response.disabled.tooltip"))

               .node("intro_1_complete")
               .startIf(context -> isOnIntro1(context) && intro1(context).filter(quest -> quest.isStarted() && quest.isCompleted()).isPresent())
               .say(translatable(INTRO_1 + "complete.1"))
               .say(translatable(INTRO_1 + "complete.2"))
               .leave().run(context -> {
                  endQuest(context, Quests.ADVISOR_INTRO_1);
                  rewardCurrency(context, 6);
               })

               .node("intro_2").startIf(AdvisorDialogue::isOnIntro2)
               .say(translatable(INTRO_2 + "page_1")).reply(translatable(INTRO_2 + "page_1.response"))
               .say(context -> translatable(INTRO_2 + "page_2", context.getVillager().getInfo().getFullName()))
               .say(translatable(INTRO_2 + "page_3"))
               .say(translatable(INTRO_2 + "page_4"))
               .say(translatable(INTRO_2 + "page_5"))
               .say(translatable(INTRO_2 + "page_6")).reply(translatable(INTRO_2 + "page_6.response"))
               .say(translatable(INTRO_2 + "page_7"))
               .say(translatable(INTRO_2 + "page_8"))
               .say(translatable(INTRO_2 + "page_9")).reply(translatable(INTRO_2 + "page_9.response"))
               .say(translatable(INTRO_2 + "page_10"))
               .say(translatable(INTRO_2 + "page_11"))
               .say(translatable(INTRO_2 + "page_12"))
               .say(translatable(INTRO_2 + "page_13"))
               .choice(translatable(INTRO_2 + "page_13.response"))
               .run(context -> completeCheckpointQuest(context, Quests.ADVISOR_INTRO_2_CHECKPOINT))
               .build();

   private AdvisorDialogue() {
   }

   private static Optional<ActivatedQuest> intro1(DialogueContext context) {
      return context.quests().tryGetQuest(Quests.ADVISOR_INTRO_1, context.getVillager().getVillagerId());
   }

   private static boolean isOnIntro1(DialogueContext context) {
      return context.quests().hasNeverCompletedQuest(Quests.ADVISOR_INTRO_1, context.getVillager().getVillagerId());
   }

   private static boolean isWaitingForIntro1(DialogueContext context) {
      return isOnIntro1(context) && intro1(context).filter(quest -> quest.isStarted() && !quest.isCompleted()).isPresent();
   }

   private static boolean isOnIntro2(DialogueContext context) {
      return !isOnIntro1(context) && context.quests()
            .hasNeverCompletedQuest(Quests.ADVISOR_INTRO_2_CHECKPOINT, context.getVillager().getVillagerId());
   }

   private static boolean isMeal(ItemStack item) {
      FoodProperties food = item.get(DataComponents.FOOD);
      return food != null && food.nutrition() >= MINIMUM_FOOD_FOR_INTRO_1;
   }
}
