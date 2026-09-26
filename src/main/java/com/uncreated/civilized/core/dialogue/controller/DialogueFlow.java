package com.uncreated.civilized.core.dialogue.controller;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.core.dialogue.IVillageDialogue;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.core.quest.attachments.PlayerDialogueCooldowns;
import com.uncreated.civilized.core.quest.attachments.PlayerQuests;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.neoforge.registration.attachments.DataAttachments;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public abstract class DialogueFlow {

   public DialogueContext buildDialogueContext(
         CivilizedVillager villager,
         Player player,
         InteractionHand hand,
         long gameTime,
         long dayTime) {
      return new DialogueContext(villager, player, gameTime, dayTime);
   }

   public final IVillageDialogue getOpeningDialogue(DialogueContext context) {
      return getOpeningDialogue(
            context,
            context.getVillager(),
            context.getPlayer(),
            context.getPlayer().getData(DataAttachments.QUESTS),
            context.getPlayer().getData(DataAttachments.DIALOGUE_COOLDOWNS));
   }

   protected abstract IVillageDialogue getOpeningDialogue(
         DialogueContext context,
         CivilizedVillager villager,
         Player player,
         PlayerQuests playerQuests,
         PlayerDialogueCooldowns cooldowns);
}
