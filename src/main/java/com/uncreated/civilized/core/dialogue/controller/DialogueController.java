package com.uncreated.civilized.core.dialogue.controller;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.dialogue.DialogueScript;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.world.entity.player.Player;

/** Picks which dialogue a villager has for a player. */
public interface DialogueController {

   @Nullable
   DialogueScript select(CivilizedVillager villager, Player player);

   static DialogueController noDialogue() {
      return (villager, player) -> null;
   }

   static DialogueController always(DialogueScript script) {
      return (villager, player) -> script;
   }
}
