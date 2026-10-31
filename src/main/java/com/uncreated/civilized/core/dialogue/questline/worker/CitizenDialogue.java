package com.uncreated.civilized.core.dialogue.questline.worker;

import static com.uncreated.civilized.core.dialogue.DialogueScript.script;
import static com.uncreated.civilized.core.dialogue.WeightedLines.weighted;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntPredicate;

import com.uncreated.civilized.core.dialogue.DialogueScript;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
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
               .say(weighted()
                     .lines("child", 3)
                     .linesAbout("family/parent", 1, CitizenDialogue::findRandomParent))
               .leave().run(CitizenDialogue::startCooldown)
               .node("chat").startIf(context -> !isOnCooldown(context))
               .say(weighted()
                     .lines("grunt", 2)
                     .lines(
                           context -> "occupation/" + info(context).getOccupation().name(),
                           context -> info(context).isHomemaker() ? 0 : 3)
                     .linesAbout("family/homemaker", 3, CitizenDialogue::findWorkingPartnerOfHomemaker)
                     .linesAbout("family/partner", 2, CitizenDialogue::findPartner)
                     .linesAbout("family/young_child", 2, context -> findRandomChild(context, true))
                     .linesAbout("family/grown_child", 1, context -> findRandomChild(context, false))
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

   private static Optional<VillagerInfo> findPartner(DialogueContext context) {
      UUID partnerId = info(context).getPartnerId();
      if (partnerId == null)
         return Optional.empty();
      return ClientVillagerStore.INSTANCE.find(partnerId).filter(partner -> !partner.isDeceased());
   }

   private static Optional<VillagerInfo> findWorkingPartnerOfHomemaker(DialogueContext context) {
      if (!info(context).isHomemaker())
         return Optional.empty();
      return findPartner(context).filter(partner -> !partner.isHomemaker());
   }

   private static Optional<VillagerInfo> findRandomChild(DialogueContext context, boolean young) {
      List<VillagerInfo> children = new ArrayList<>();
      for (VillagerInfo child : ClientVillagerStore.INSTANCE.findChildren(info(context).getVillagerId())) {
         if (!child.isDeceased() && child.isChild() == young)
            children.add(child);
      }
      return pickRandom(context, children);
   }

   private static Optional<VillagerInfo> findRandomParent(DialogueContext context) {
      List<VillagerInfo> parents = new ArrayList<>();
      for (UUID parentId : info(context).getParentIds())
         ClientVillagerStore.INSTANCE.find(parentId).filter(parent -> !parent.isDeceased()).ifPresent(parents::add);
      return pickRandom(context, parents);
   }

   private static Optional<VillagerInfo> pickRandom(DialogueContext context, List<VillagerInfo> villagers) {
      if (villagers.isEmpty())
         return Optional.empty();
      return Optional.of(villagers.get(context.getVillager().getRandom().nextInt(villagers.size())));
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
