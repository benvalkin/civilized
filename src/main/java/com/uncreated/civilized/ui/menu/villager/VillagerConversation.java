package com.uncreated.civilized.ui.menu.villager;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.core.dialogue.Dialogue;
import com.uncreated.civilized.core.dialogue.IVillageDialogue;
import com.uncreated.civilized.core.dialogue.OpeningDialogue;
import com.uncreated.civilized.core.dialogue.ResponseOption;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.dialogue.context.ResponseOptionContext;
import com.uncreated.civilized.core.dialogue.specialized.ItemDepotDialogue;
import com.uncreated.civilized.entity.CivilizedVillager;

import lombok.Getter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

@Getter
public class VillagerConversation {

   @Nullable
   private IVillageDialogue dialogue;
   @Nullable
   private final DialogueContext context;

   private VillagerConversation(@Nullable IVillageDialogue dialogue, @Nullable DialogueContext context) {
      this.dialogue = dialogue;
      this.context = context;
   }

   public static VillagerConversation start(CivilizedVillager villager, Player player, InteractionHand hand) {
      @Nullable
      OpeningDialogue openingDialogue = villager.findOpeningDialogue(player, hand);
      if (openingDialogue == null)
         return new VillagerConversation(null, null);

      // an item depot dialogue only takes items. The screen only opens for one when the player's hand was empty, so
      // there's nothing to say
      @Nullable
      IVillageDialogue dialogue = openingDialogue.dialogue();
      if (dialogue instanceof ItemDepotDialogue)
         dialogue = null;

      return new VillagerConversation(dialogue, openingDialogue.context());
   }

   public boolean hasDialogue() {
      return dialogue != null && context != null;
   }

   /**
    * Executes the response the player picked, and returns the next dialogue step decision.
    */
   public ResponseOption.SelectedAction respond(ResponseOptionContext optionContext) {
      ResponseOption.SelectedAction result =
            optionContext.getSelectedOption().getOnPress().onOptionSelected(optionContext);

      if (dialogue == null || result == ResponseOption.SelectedAction.DO_NOTHING)
         return ResponseOption.SelectedAction.DO_NOTHING;

      dialogue.getOnEnded().accept(context);
      if (result == ResponseOption.SelectedAction.CLOSE_DIALOGUE)
         return ResponseOption.SelectedAction.CLOSE_DIALOGUE;

      @Nullable
      Dialogue next = null;
      // response option's dialogue takes precedence over pages
      if (optionContext.getSelectedOption().getNextDialogue() != null)
         next = optionContext.getSelectedOption().getNextDialogue();
      else if (dialogue.hasNextPage())
         next = dialogue.getNextPage();

      // check availability and fallbacks
      while (next != null && !next.isAvailableToPlayer(context)) {
         next = next.getFallback();
      }

      // finally, if there is no more dialogue to display, the conversation is over
      if (next == null)
         return ResponseOption.SelectedAction.CLOSE_DIALOGUE;

      dialogue = next;
      return ResponseOption.SelectedAction.GO_NEXT;
   }
}
