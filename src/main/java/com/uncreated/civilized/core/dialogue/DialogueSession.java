package com.uncreated.civilized.core.dialogue;

import static net.minecraft.network.chat.Component.translatable;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.ui.style.Colors;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** A dialogue that the player is having with a villager. Advances through the dialogue script. */
public class DialogueSession {

   private static final DialogueChoice DEFAULT_LEAVE =
         new DialogueChoice(
               context -> translatable("villager.dialogue.response.close")
                     .withColor(Colors.MENU_TEXT_VILLAGER_DIALOGUE_ACTION),
               false);

   private final DialogueScript script;
   @Getter
   private final DialogueContext context;
   private DialogueNode node;
   private final List<Page> pages = new ArrayList<>();
   private int pageIndex;

   private DialogueSession(DialogueScript script, DialogueContext context, DialogueNode start) {
      this.script = script;
      this.context = context;
      enter(start);
   }

   /** @return null if the villager has nothing to say to the player */
   public static @Nullable DialogueSession start(CivilizedVillager villager, Player player) {
      DialogueScript script = findScript(villager, player);
      if (script == null)
         return null;

      DialogueContext context = new DialogueContext(villager, player);
      DialogueNode start = script.findStart(context);
      return start == null ? null : new DialogueSession(script, context, start);
   }

   public static @Nullable DialogueScript findScript(CivilizedVillager villager, Player player) {
      return villager.getInfo().getNpcRole().dialogue().select(villager, player);
   }

   public Component speech() {
      return pages.get(pageIndex).text();
   }

   /**
    * @return a list of dialogue choices for the current page that the player can click on. Choices that are not visible
    *         are omitted from the list.
    */
   public List<DisplayedChoice> choicesForCurrentPage() {
      Page page = pages.get(pageIndex);
      if (pageIndex < pages.size() - 1) {
         Component reply =
               page.reply() != null ? page.reply()
                     : translatable("villager.dialogue.response.next_page")
                           .withColor(Colors.MENU_TEXT_VILLAGER_DIALOGUE_ACTION);
         return List.of(new DisplayedChoice(new DialogueChoice(context -> reply, true), reply, ChoiceState.enabled()));
      }

      DialogueNode choicesNode = node.getChoicesFromOtherNode() != null ? script.node(node.getChoicesFromOtherNode()) : node;
      List<DialogueChoice> nodeChoices = choicesNode.choices;
      // the last speech's reply ends the dialogue when there's nothing else to pick
      if (nodeChoices.isEmpty())
         nodeChoices =
               List.of(page.reply() != null ? new DialogueChoice(context -> page.reply(), false) : DEFAULT_LEAVE);

      List<DisplayedChoice> shown = new ArrayList<>();
      for (DialogueChoice choice : nodeChoices) {
         ChoiceState state = choice.checkState(context);
         if (state.isVisible())
            shown.add(new DisplayedChoice(choice, choice.getText().apply(context), state));
      }
      return shown;
   }

   /**
    * Actions the specified dialogue choice that the player clicked on.
    * 
    * @return true if there's more dialogue to follow after this, false if it should end.
    */
   public boolean choose(DialogueChoice choice) {
      if (choice.isGoesToNextPage()) {
         pageIndex++;
         return true;
      }

      for (DialogueAction action : choice.getActions())
         action.runOnClient(context);

      if (choice.getTarget() == null)
         return false;

      enter(script.node(choice.getTarget()));
      return true;
   }

   private void enter(DialogueNode next) {
      node = next;
      pageIndex = 0;
      pages.clear();
      for (DialogueNode.Speech speech : next.speeches) {
         List<Component> texts = speech.text.pages(script, context);
         for (int i = 0; i < texts.size(); i++)
            pages.add(new Page(texts.get(i), i == texts.size() - 1 ? speech.reply : null));
      }
   }

   private record Page(Component text, @Nullable Component reply) {
   }

   public record DisplayedChoice(DialogueChoice choice, Component text, ChoiceState state) {
   }
}
