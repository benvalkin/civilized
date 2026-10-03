package com.uncreated.civilized.core.dialogue.controller;

import java.util.List;

import javax.annotation.Nullable;

import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public abstract class DialogueController {

   public DialogueController() {

   }

   public abstract @Nullable DialogueFlow getDialogueFlow(
         CivilizedVillager villager,
         Player player,
         InteractionHand hand);

   public static DialogueController noDialogue() {
      return new DialogueController() {
         @Override
         public @Nullable DialogueFlow getDialogueFlow(
               CivilizedVillager villager,
               Player player,
               InteractionHand hand) {
            return null;
         }
      };
   }

   public static DialogueController selectDialogueController(CivilizedVillager villager) {
      return villager.getInfo().getNpcRole().createDialogueController();
   }
}
