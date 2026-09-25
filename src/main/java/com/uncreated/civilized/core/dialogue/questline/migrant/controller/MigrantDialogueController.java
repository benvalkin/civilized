package com.uncreated.civilized.core.dialogue.questline.migrant.controller;

import java.util.Optional;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.dialogue.controller.DialogueController;
import com.uncreated.civilized.core.dialogue.controller.DialogueFlow;
import com.uncreated.civilized.core.dialogue.questline.migrant.MigrantGenericQuest;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public class MigrantDialogueController extends DialogueController {

   private static final SimpleWeightedRandomList<DialogueFlow> FLOW_OPTIONS =
         new SimpleWeightedRandomList.Builder<DialogueFlow>().add(new MigrantGenericQuest(), 1).build();

   @Override
   public @Nullable DialogueFlow getDialogueFlow(CivilizedVillager villager, Player player, InteractionHand hand) {
      Optional<DialogueFlow> dialogueFlow = FLOW_OPTIONS.getRandomValue(villager.getConsistentLifetimeRandom());
      return dialogueFlow.orElse(null);

   }
}
