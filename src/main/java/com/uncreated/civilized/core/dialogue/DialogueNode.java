package com.uncreated.civilized.core.dialogue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.dialogue.context.DialogueContext;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

/**
 * A single overarching step of a dialogue. A node has a unique name for identification, as well as two renderable
 * components: i) villager speech ii) dialogue choices that display when all the speech pages are done.
 */
@Getter
@Setter(AccessLevel.PACKAGE)
public class DialogueNode {
   private final String name;
   final List<Speech> speeches = new ArrayList<>();
   final List<DialogueChoice> choices = new ArrayList<>();
   /** If specified, shows another node's choices instead of this node's choices. */
   private @Nullable String choicesFromOtherNode;
   /**
    * If specified, a dialogue node will not start unless. If it's the start of the dialogue and this check if false,
    * another start node will be chosen instead of this one. If there are no other nodes, a dialogue session will not
    * start.
    */
   private @Nullable Predicate<DialogueContext> startCondition;

   DialogueNode(String name) {
      this.name = name;
   }

   /** A page of speech + the choice that goes to the next page. */
   static class Speech {
      final DialogueText text;
      @Nullable
      Component reply;

      Speech(DialogueText text) {
         this.text = text;
      }
   }
}
