package com.uncreated.civilized.client;

import com.uncreated.civilized.core.dialogue.IVillageDialogue;
import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.ui.menu.dialogue.VillagerDialogueScreen;

import net.minecraft.client.Minecraft;

/**
 * Opens the dialogue screen for a villager. Kept out of {@link CivilizedVillager} so that the villager never refers to
 * a screen, which would crash a dedicated server as soon as the villager class loads.
 */
public final class VillagerDialogueClientHandler {

   private VillagerDialogueClientHandler() {
   }

   public static void openDialogue(CivilizedVillager villager, IVillageDialogue dialogue, DialogueContext context) {
      Minecraft.getInstance().setScreen(new VillagerDialogueScreen(villager, dialogue, context));
   }
}
