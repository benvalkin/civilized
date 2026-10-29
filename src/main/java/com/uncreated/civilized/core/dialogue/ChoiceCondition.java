package com.uncreated.civilized.core.dialogue;

import com.uncreated.civilized.core.dialogue.context.DialogueContext;

public interface ChoiceCondition {
   ChoiceState check(DialogueContext context);
}
