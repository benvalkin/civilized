package com.uncreated.civilized.core.dialogue.questline.worker;

import static com.uncreated.civilized.core.dialogue.Dialogue.dialogue;
import static com.uncreated.civilized.core.dialogue.DialogueWithPages.dialogueWithpages;
import static com.uncreated.civilized.core.dialogue.RandomSpeech.randomOneOf;
import static com.uncreated.civilized.core.dialogue.ResponseOption.closeDialogue;
import static net.minecraft.network.chat.Component.translatable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.uncreated.civilized.core.dialogue.IVillageDialogue;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.dialogue.controller.DialogueFlow;
import com.uncreated.civilized.core.quest.attachments.PlayerDialogueCooldowns;
import com.uncreated.civilized.core.quest.attachments.PlayerQuests;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.world.entity.player.Player;

public class WorkerDialogueFlow extends DialogueFlow {

   public static final String DIALOGUE_KEY = "worker_chat_simple";

   @Override
   protected IVillageDialogue getOpeningDialogue(
         DialogueContext context,
         CivilizedVillager villager,
         Player player,
         PlayerQuests playerQuests,
         PlayerDialogueCooldowns dialogueCooldowns) {

      if (dialogueCooldowns.hasCooldown(villager, DIALOGUE_KEY, context.getGameTime()))
         return null;

      return dialogueWithpages()
            .page(
                  dialogue(
                        randomOneOf(
                              translatable("villager.dialogue.grunt.worker.generic.1"),
                              translatable("villager.dialogue.grunt.worker.generic.2"),
                              translatable("villager.dialogue.grunt.worker.generic.3"),
                              translatable("villager.dialogue.grunt.worker.generic.4"),
                              translatable("villager.dialogue.grunt.worker.generic.5"),
                              translatable("villager.dialogue.grunt.worker.generic.6"),
                              translatable("villager.dialogue.grunt.worker.generic.7"),
                              translatable("villager.dialogue.grunt.worker.generic.8"),
                              translatable("villager.dialogue.grunt.worker.generic.9"),
                              translatable("villager.dialogue.grunt.worker.generic.10"),
                              translatable("villager.dialogue.grunt.worker.generic.11"),
                              translatable("villager.dialogue.grunt.worker.generic.12"),
                              translatable("villager.dialogue.grunt.worker.generic.13"),
                              translatable("villager.dialogue.grunt.worker.generic.14"),
                              translatable("villager.dialogue.grunt.worker.generic.15"),
                              translatable("villager.dialogue.grunt.worker.generic.16"))))
            .create()
            .showIf(ctx -> !dialogueCooldowns.hasCooldown(villager, DIALOGUE_KEY, context.getGameTime()))
            .onEnded(
                  ctx -> dialogueCooldowns.startCooldown(
                        villager,
                        DIALOGUE_KEY,
                        Duration.of(1, ChronoUnit.MINUTES),
                        context.getGameTime()))
            .response(closeDialogue());
   }
}
