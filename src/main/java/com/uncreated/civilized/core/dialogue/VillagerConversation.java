package com.uncreated.civilized.core.dialogue;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.dialogue.context.ResponseOptionContext;
import com.uncreated.civilized.core.dialogue.controller.DialogueController;
import com.uncreated.civilized.core.dialogue.controller.DialogueFlow;
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

   public static VillagerConversation openingDialogue(CivilizedVillager villager, Player player, InteractionHand hand) {
      DialogueController dialogueController = DialogueController.selectDialogueController(villager);
      @Nullable
      DialogueFlow dialogueFlow = dialogueController.getDialogueFlow(villager, player, hand);
      if (dialogueFlow == null)
         return new VillagerConversation(null, null);

      long gameTime = player.level().getGameTime();
      long dayTime = player.level().getDayTime();
      DialogueContext context = dialogueFlow.buildDialogueContext(villager, player, hand, gameTime, dayTime);

      @Nullable
      IVillageDialogue dialogue = dialogueFlow.getOpeningDialogue(context);
      while (dialogue != null && !dialogue.isAvailableToPlayer(context)) {
         dialogue = dialogue.getFallback();
      }

      return new VillagerConversation(dialogue, context);
   }

   public boolean hasAnySpeech() {
      return dialogue != null && context != null && !(dialogue instanceof ItemDepotDialogue);
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
