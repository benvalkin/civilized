package com.uncreated.civilized.core.dialogue;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.core.dialogue.context.DialogueContext;

public record OpeningDialogue(@Nullable IVillageDialogue dialogue, DialogueContext context) {
    // todo: this is the same record class as `VillagerConversation`. I think we should merge the two?
}
