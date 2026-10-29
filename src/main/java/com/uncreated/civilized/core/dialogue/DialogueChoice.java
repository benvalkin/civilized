package com.uncreated.civilized.core.dialogue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.dialogue.context.DialogueContext;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

@Getter
@Setter(AccessLevel.PACKAGE)
public class DialogueChoice {
   private final Function<DialogueContext, Component> text;
   private ChoiceCondition condition = context -> ChoiceState.enabled();
   private final List<DialogueAction> actions = new ArrayList<>();
   /**
    * A new dialogue to advance to when this choice is chosen. If not specified, the dialogue will end when this choice
    * is chosen.
    */
   private @Nullable String target;
   private @Nullable Component tooltip;
   /** Moves on to the node's next page, rather than a new node */
   private final boolean goesToNextPage;

   DialogueChoice(Function<DialogueContext, Component> text, boolean goesToNextPage) {
      this.text = text;
      this.goesToNextPage = goesToNextPage;
   }

   public ChoiceState checkState(DialogueContext context) {
      ChoiceState state = condition.check(context);
      if (state.tooltip() == null && tooltip != null)
         return new ChoiceState(state.isVisible(), state.isEnabled(), tooltip);
      return state;
   }
}
