package com.uncreated.civilized.core.dialogue.questline.migrant;

import static com.uncreated.civilized.core.dialogue.DialogueLines.text;
import static net.minecraft.network.chat.Component.translatable;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.dialogue.ChoiceCondition;
import com.uncreated.civilized.core.dialogue.ChoiceState;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.recruitment.AttractivenessSource;
import com.uncreated.civilized.core.recruitment.Impression;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** The visitor must think highly enough of the player's settlement, and there must be work for them there. */
public class CanJoinSettlementCheck implements ChoiceCondition {

   private static final String TOOLTIPS = "recruitment/tooltips";

   @Override
   public ChoiceState check(DialogueContext context) {
      Optional<Settlement> settlement = WelcomeCheck.findPlayersSettlement(context);
      if (settlement.isEmpty()) {
         return ChoiceState.disabled(
               translatable("villager.dialogue.traveller.quest.validation.tooltip.player_no_settlement")
                     .withColor(Colors.VALIDATION_ERROR));
      }

      UUID settlementId = settlement.get().getSettlementId();
      VillagerInfo visitor = context.getVillager().getInfo();
      Impression impression =
            Impression.of(visitor, settlementId, ClientBuildingStore.INSTANCE, ClientVillagerStore.INSTANCE);
      MutableComponent tooltip = breakdown(impression);

      if (!impression.isWillingToJoin()) {
         tooltip.append("\n\n")
               .append(
                     text(TOOLTIPS, "not_willing", visitor.getFirstName(), impression.required())
                           .withColor(Colors.VALIDATION_ERROR))
               .append("\n")
               .append(impressionNeededTooltip(impression));
         return ChoiceState.disabled(tooltip);
      }

      // todo: we might want to allow them to join anyway - the thinking is that they can just be
      // unemployed/homeless until the player sorts them out. they'd get mood debuffs from this.
      if (BuildingUtil
            .findWorksiteFor(
                  visitor.getOccupation(),
                  settlementId,
                  ClientBuildingStore.INSTANCE,
                  ClientVillagerStore.INSTANCE)
            .isEmpty()) {
         tooltip.append("\n")
               .append(
                     translatable("villager.dialogue.quest.migrant_worker.misc.response.accept.tooltip.option_disabled")
                           .withColor(Colors.VALIDATION_ERROR));
         return ChoiceState.disabled(tooltip);
      }

      tooltip.append("\n\n")
            .append(text(TOOLTIPS, "willing", visitor.getFirstName()).withColor(Colors.VALIDATION_SUCCESS));
      return ChoiceState.enabled(tooltip);
   }

   private static MutableComponent breakdown(Impression impression) {
      MutableComponent tooltip = text(TOOLTIPS, "impression", impression.total());
      for (AttractivenessSource source : impression.attractiveness())
         tooltip.append("\n")
               .append(text(TOOLTIPS, "source", source.name(), source.value()).withColor(Colors.TEXT_LIGHT_MUTED));

      if (impression.hospitality() > 0)
         tooltip.append("\n").append(text(TOOLTIPS, "hospitality", impression.hospitality()).withColor(Colors.FAMILY));
      return tooltip;
   }

   private static @NotNull MutableComponent impressionNeededTooltip(Impression impression) {
      return text(
            TOOLTIPS,
            "impression_needed",
            Component.literal(String.valueOf(impression.required())).withStyle(ChatFormatting.WHITE))
            .withColor(Colors.TEXT_LIGHT_MUTED);
   }
}
