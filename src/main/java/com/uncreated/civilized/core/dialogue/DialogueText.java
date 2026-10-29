package com.uncreated.civilized.core.dialogue;

import java.util.List;

import com.uncreated.civilized.core.dialogue.context.DialogueContext;

import net.minecraft.network.chat.Component;

/** Something a villager says, looked up when it's shown. A line from a file can be several pages long. */
public interface DialogueText {
   List<Component> pages(DialogueScript script, DialogueContext context);
}
