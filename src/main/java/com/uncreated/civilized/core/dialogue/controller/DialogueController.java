package com.uncreated.civilized.core.dialogue.controller;

import java.util.List;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.dialogue.questline.advisor.controller.AdvisorDialogueController;
import com.uncreated.civilized.core.dialogue.questline.migrant.controller.MigrantDialogueController;
import com.uncreated.civilized.core.dialogue.questline.worker.controller.WorkerDialogueController;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRole;
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
      DialogueController controller;
      List<VillagerNpcRole> roles = villager.getInfo().getNpcRoles();

      if (roles.contains(VillagerNpcRole.WORKER)) {
         controller = new WorkerDialogueController();
      } else if (roles.contains(VillagerNpcRole.ADVISOR)) {
         controller = new AdvisorDialogueController();
      } else if (roles.contains(VillagerNpcRole.MIGRANT)) {
         controller = new MigrantDialogueController();
      } else
         return DialogueController.noDialogue();

      return controller;
   }
}
