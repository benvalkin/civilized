package com.uncreated.civilized.core.dialogue;

import com.uncreated.civilized.core.dialogue.context.DialogueContext;

/** Runs on the client when a choice is picked. */
public interface DialogueAction {
   void runOnClient(DialogueContext context);
}
