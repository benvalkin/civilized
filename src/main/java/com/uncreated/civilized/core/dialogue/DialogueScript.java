package com.uncreated.civilized.core.dialogue;

import static net.minecraft.network.chat.Component.translatable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.ui.style.Colors;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

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
   /** A file in the , when the script's lines are split into sets. */
   private final @Nullable String linesFileName;
   private final Map<String, DialogueNode> nodes;

   private DialogueScript(Builder builder) {
      this.linesFolder = builder.linesFolder;
      this.linesFileName = builder.linesFileName;
      this.nodes = builder.nodes;
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

   /**
    * Specifies the file name which contains the dialogue lines of this script. A lines file can either be inside the
    * root folder, or inside a numbered variation subfolder '/1', '/2', etc. to make each villager choose a particular
    * random variation of the same lines.
    */
   public String linesFileName(DialogueContext context, String file) {
      if (linesFolder == null)
         throw new IllegalStateException("A dialogue without a lines folder can't look up \"" + file + "\"");

      if (linesFileName != null) {
         String inSet =
               DialogueLines.chooseSetPerVillager(context.getVillager(), linesFolder, linesFileName) + "/" + file;
         if (DialogueLines.entryCount(inSet) > 0)
            return inSet;
      }
      return linesFolder + "/" + file;
   }

   public static class Builder {

      private final @Nullable String linesFolder;
      private @Nullable String linesFileName;
      private final Map<String, DialogueNode> nodes = new LinkedHashMap<>();

      private @Nullable DialogueNode node;
      private @Nullable DialogueNode.Speech speech;
      private @Nullable DialogueChoice choice;

      private Builder(@Nullable String linesFolder) {
         this.linesFolder = linesFolder;
      }

      /**
       * Specifies the file name which contains the dialogue lines of this script. A lines file can either be inside the
       * root folder, or inside a numbered variation subfolder '/1', '/2', etc. to make each villager choose a
       * particular random variation of the same lines.
       */
      public Builder linesFileName(String linesFileName) {
         this.linesFileName = linesFileName;
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
               (script, context) -> DialogueLines
                     .randomPages(context.getVillager(), script.linesFileName(context, file)));
      }

      public Builder sayDeterministicPerVillager(String file) {
         return say(
               (script, context) -> DialogueLines
                     .randomPagesPerVillager(context.getVillager(), script.linesFileName(context, file)));
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

      /** A choice taken from the specified {@code choices} file that matches the specified {@code choiceName}. */
      public Builder choice(String choiceName) {
         return choice(context -> DialogueLines.text(choicesFile(), choiceName));
      }

      public Builder choice(String choiceName, UnaryOperator<MutableComponent> formatChosenLine) {
         return choice(context -> formatChosenLine.apply(DialogueLines.text(choicesFile(), choiceName)));
      }

      public Builder choiceAction(String choiceName) {
         return choice(
               context -> DialogueLines.text(choicesFile(), choiceName)
                     .withColor(Colors.MENU_TEXT_VILLAGER_DIALOGUE_ACTION));
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
