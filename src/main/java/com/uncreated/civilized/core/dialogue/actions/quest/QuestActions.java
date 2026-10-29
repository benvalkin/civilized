package com.uncreated.civilized.core.dialogue.actions.quest;

import com.uncreated.civilized.core.dialogue.DialogueAction;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.quest.QuestType;

public class QuestActions {

   public static DialogueAction startQuest(QuestType questType) {
      return context -> context.quests().tryStartQuest(questType, context.vendorIdFor(questType));
   }

   public static void completeQuest(DialogueContext context, QuestType questType) {
      context.quests().completeQuest(questType, context.vendorIdFor(questType));
   }

   public static void endQuest(DialogueContext context, QuestType questType) {
      context.quests().endQuest(questType, context.vendorIdFor(questType));
   }

   public static void completeCheckpointQuest(DialogueContext context, QuestType questType) {
      context.quests().completeCheckpointQuest(questType, context.vendorIdFor(questType));
   }
}
