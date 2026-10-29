package com.uncreated.civilized.core.dialogue.questline.worker;

import static com.uncreated.civilized.core.dialogue.DialogueScript.script;

import java.time.Duration;

import com.uncreated.civilized.core.dialogue.DialogueScript;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;

public final class WorkerDialogue {

   private static final String COOLDOWN_KEY = "worker_chat_simple";

   public static final DialogueScript SCRIPT =
         script("dialogue/worker")
               .node("chat").startIf(context -> !isOnCooldown(context)).say("grunt")
               .leave().run(WorkerDialogue::startCooldown)
               .build();

   private WorkerDialogue() {
   }

   private static boolean isOnCooldown(DialogueContext context) {
      return context.cooldowns().hasCooldown(context.getVillager(), COOLDOWN_KEY, context.getGameTime());
   }

   private static void startCooldown(DialogueContext context) {
      context.cooldowns()
            .startCooldown(context.getVillager(), COOLDOWN_KEY, Duration.ofMinutes(1), context.getGameTime());
   }
}
