package com.uncreated.civilized.core.dialogue.questline.worker;

import static com.uncreated.civilized.core.dialogue.DialogueScript.script;
import static com.uncreated.civilized.core.dialogue.WeightedLines.weighted;

import java.time.Duration;
import java.util.function.IntPredicate;

import com.uncreated.civilized.core.dialogue.DialogueScript;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkill;
import com.uncreated.civilized.entity.data.VillagerHunger;

public final class CitizenDialogue {

   private static final String COOLDOWN_KEY = "worker_chat_simple";
   private static final int UNSKILLED_LEVEL = 2;
   private static final int SKILLED_LEVEL = 8;

   // @formatter:off
   public static final DialogueScript SCRIPT =
         script("dialogue/citizen")
               .node("child_chat").startIf(context -> !isOnCooldown(context) && info(context).isChild())
               .say("child")
               .leave().run(CitizenDialogue::startCooldown)
               .node("chat").startIf(context -> !isOnCooldown(context))
               .say(weighted()
                     .lines("grunt", 2)
                     .lines(context -> "occupation/" + info(context).getOccupation().name(), 3)
                     .lines("hungry", CitizenDialogue::hungerWeight)
                     .lines("homeless", context -> isHomeless(context) ? 3 : 0)
                     .lines("raining", context -> context.getPlayer().level().isRaining() ? 2 : 0)
                     .lines("unskilled", context -> isWorkSkill(context, level -> level <= UNSKILLED_LEVEL) ? 2 : 0)
                     .lines("skilled", context -> isWorkSkill(context, level -> level >= SKILLED_LEVEL) ? 2 : 0))
               .leave().run(CitizenDialogue::startCooldown)
               .build();
   // @formatter:on

   private CitizenDialogue() {
   }

   private static VillagerInfo info(DialogueContext context) {
      return context.getVillager().getInfo();
   }

   private static int hungerWeight(DialogueContext context) {
      VillagerHunger hunger = context.getVillager().getHunger();
      if (hunger.isHungryEnoughToStrike())
         return 8;
      return hunger.isHungry() ? 3 : 0;
   }

   private static boolean isHomeless(DialogueContext context) {
      return info(context).getSettlementId() != null && info(context).getHomeBuildingId() == null;
   }

   private static boolean isWorkSkill(DialogueContext context, IntPredicate levelCondition) {
      VillagerSkill skill = info(context).getOccupation().relevantSkill();
      return skill != null && levelCondition.test(info(context).getSkills().getLevel(skill));
   }

   private static boolean isOnCooldown(DialogueContext context) {
      return context.cooldowns().hasCooldown(context.getVillager(), COOLDOWN_KEY, context.getGameTime());
   }

   private static void startCooldown(DialogueContext context) {
      context.cooldowns()
            .startCooldown(context.getVillager(), COOLDOWN_KEY, Duration.ofMinutes(1), context.getGameTime());
   }
}
