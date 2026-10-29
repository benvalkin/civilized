package com.uncreated.civilized.core.dialogue;

import static net.minecraft.network.chat.Component.translatable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.ui.style.Colors;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A villager dialogue script consists of named nodes. Each node says a dialogue line (a line can be split into pages)
 * and then offers choices to the player. Choices can lead to other nodes by linking their names. Dialogue scripts are
 * built beforehand, and the Dialogue GUI looks up and displays translations as the script progresses.
 *
 * <pre>
 * script("dialogue/vagrant").sets("grunt")
 *       .node("opening")
 *       .say("grunt")
 *       .choice("offer_food")
 *       .when(new GiveFoodCheck(4))
 *       .runOnServer(GIVE_FOOD)
 *       .then("fed")
 *       .leave()
 *       .node("fed")
 *       .say("fed")
 *       .choicesFrom("opening")
 *       .build();
 * </pre>
 *
 * Choice modifiers ({@code when}, {@code run}, {@code then}, ...) apply to the last choice that was added to the script
 * * builder. {@code reply} applies to the last say() that was added.
 */
@Getter
public class DialogueScript {

   private static final String CHOICES_FILE = "choices";

   /** Where the script's lines files are, e.g. {@code "dialogue/vagrant"}. */
   private final @Nullable String linesFolder;
   /** A file in every numbered set folder, when the script's lines are split into sets. */
   private final @Nullable String fileInEverySet;
   private final Map<String, DialogueNode> nodes;
   private final @Nullable ItemOffer itemOffer;

   private DialogueScript(Builder builder) {
      this.linesFolder = builder.linesFolder;
      this.fileInEverySet = builder.commonFile;
      this.nodes = builder.nodes;
      this.itemOffer = builder.itemOffer;
   }

   public static Builder script() {
      return new Builder(null);
   }

   public static Builder script(String linesFolder) {
      return new Builder(linesFolder);
   }

   public DialogueNode node(String name) {
      DialogueNode node = nodes.get(name);
      if (node == null)
         throw new IllegalArgumentException("Dialogue has no node called " + name);
      return node;
   }

   public @Nullable DialogueNode findStart(DialogueContext context) {
      boolean anyStartConditions = false;
      for (DialogueNode node : nodes.values()) {
         if (node.getStartCondition() == null)
            continue;

         anyStartConditions = true;
         if (node.getStartCondition().test(context))
            return node;
      }

      if (anyStartConditions || nodes.isEmpty())
         return null;
      return nodes.values().iterator().next();
   }

   /** The path of one of this script's lines files, from the villager's set if they have one. */
   public String linesFile(DialogueContext context, String file) {
      if (linesFolder == null)
         throw new IllegalStateException("A dialogue without a lines folder can't look up \"" + file + "\"");

      if (fileInEverySet != null) {
         String inSet =
               DialogueLines.chooseSetPerVillager(context.getVillager(), linesFolder, fileInEverySet) + "/" + file;
         if (DialogueLines.entryCount(inSet) > 0)
            return inSet;
      }
      return linesFolder + "/" + file;
   }

   /** Taking an item from a player who right clicks the villager with it, instead of talking. Runs on the server. */
   public record ItemOffer(ItemOfferCheck accepts, ItemOfferAction take) {
   }

   public interface ItemOfferCheck {
      boolean accepts(DialogueContext context, ItemStack offered);
   }

   public interface ItemOfferAction {
      void take(DialogueContext context, ItemStack offered);
   }

   public static class Builder {

      private final @Nullable String linesFolder;
      private @Nullable String commonFile;
      private final Map<String, DialogueNode> nodes = new LinkedHashMap<>();
      private @Nullable ItemOffer itemOffer;

      private @Nullable DialogueNode node;
      private @Nullable DialogueNode.Speech speech;
      private @Nullable DialogueChoice choice;

      private Builder(@Nullable String linesFolder) {
         this.linesFolder = linesFolder;
      }

      /**
       * The lines are split into numbered set folders ({@code 1/}, {@code 2/}, ...) and each villager sticks to one of
       * them. Files missing from a set are looked up in the main folder.
       */
      public Builder commonFile(String commonFile) {
         this.commonFile = commonFile;
         return this;
      }

      public Builder acceptsItem(ItemOfferCheck accepts, ItemOfferAction take) {
         this.itemOffer = new ItemOffer(accepts, take);
         return this;
      }

      public Builder node(String name) {
         if (nodes.containsKey(name))
            throw new IllegalStateException("Dialogue already has a node called " + name);

         node = new DialogueNode(name);
         nodes.put(name, node);
         speech = null;
         choice = null;
         return this;
      }

      /**
       * The dialogue can start at this node. The first node whose condition passes is started at, and if no node has a
       * condition, the dialogue starts at the first node.
       */
      public Builder startIf(Predicate<DialogueContext> condition) {
         currentNode().setStartCondition(condition);
         return this;
      }

      public Builder say(String file) {
         return say(
               (script, context) -> DialogueLines.randomPages(context.getVillager(), script.linesFile(context, file)));
      }

      public Builder sayDeterministicPerVillager(String file) {
         return say(
               (script, context) -> DialogueLines
                     .randomPagesPerVillager(context.getVillager(), script.linesFile(context, file)));
      }

      public Builder say(Component text) {
         return say((script, context) -> List.of(text));
      }

      /** One of the given lines, picked at random each time. */
      public Builder sayOneOf(Component... options) {
         return say((script, context) -> List.of(options[context.getVillager().getRandom().nextInt(options.length)]));
      }

      public Builder say(Function<DialogueContext, Component> text) {
         return say((script, context) -> List.of(text.apply(context)));
      }

      public Builder say(DialogueText text) {
         speech = new DialogueNode.Speech(text);
         currentNode().speeches.add(speech);
         return this;
      }

      /** Customized response text that shows instead of "next". */
      public Builder reply(Component text) {
         if (speech == null)
            throw new IllegalStateException("reply() needs something said before it");
         speech.reply = text;
         return this;
      }

      /** A choice whose text is in the lines folder's {@code choices} file. */
      public Builder choice(String name) {
         return choice(context -> DialogueLines.text(choicesFile(), name));
      }

      public Builder choice(Component text) {
         return choice(context -> text);
      }

      public Builder choice(Function<DialogueContext, Component> text) {
         choice = new DialogueChoice(text, false);
         currentNode().choices.add(choice);
         return this;
      }

      /** Ends the dialogue with . */
      public Builder leave() {
         return choice(
               translatable("villager.dialogue.response.close").withColor(Colors.MENU_TEXT_VILLAGER_DIALOGUE_ACTION));
      }

      public Builder when(ChoiceCondition condition) {
         currentChoice().setCondition(condition);
         return this;
      }

      public Builder tooltip(Component tooltip) {
         currentChoice().setTooltip(tooltip);
         return this;
      }

      public Builder run(DialogueAction action) {
         currentChoice().getActions().add(action);
         return this;
      }

      public Builder runOnServer(ResourceLocation serverAction) {
         return run(context -> ServerDialogueActions.runOnServer(serverAction));
      }

      /** Advances to a different dialogue node after the current choice is clicked. */
      public Builder then(String nodeName) {
         currentChoice().setTarget(nodeName);
         return this;
      }

      /** Shows the choices of another node. They are checked again */
      public Builder choicesFromOtherNode(String otherNodeName) {
         currentNode().setChoicesFromOtherNode(otherNodeName);
         return this;
      }

      public DialogueScript build() {
         for (DialogueNode built : nodes.values()) {
            if (built.speeches.isEmpty())
               throw new IllegalStateException("Dialogue node " + built.getName() + " doesn't say anything");
            checkExists(built.getChoicesFromOtherNode());
            for (DialogueChoice builtChoice : built.choices)
               checkExists(builtChoice.getTarget());
         }
         return new DialogueScript(this);
      }

      private void checkExists(@Nullable String nodeName) {
         if (nodeName != null && !nodes.containsKey(nodeName))
            throw new IllegalStateException("Dialogue has no node called " + nodeName);
      }

      private String choicesFile() {
         if (linesFolder == null)
            throw new IllegalStateException("A dialogue without a lines folder can't look up choices");
         return linesFolder + "/" + CHOICES_FILE;
      }

      private DialogueNode currentNode() {
         if (node == null)
            throw new IllegalStateException("Start a node() first");
         return node;
      }

      private DialogueChoice currentChoice() {
         if (choice == null)
            throw new IllegalStateException("Add a choice() first");
         return choice;
      }
   }
}
