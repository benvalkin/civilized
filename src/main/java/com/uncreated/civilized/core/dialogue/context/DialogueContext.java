package com.uncreated.civilized.core.dialogue.context;

import java.util.HashMap;

import com.uncreated.civilized.core.dialogue.IVillageDialogue;
import com.uncreated.civilized.entity.CivilizedVillager;

import lombok.Getter;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

@Getter
public class DialogueContext {
   protected final CivilizedVillager villager;
   protected final Player player;
    private final long gameTime;
    private final long dayTime;

    public DialogueContext(CivilizedVillager villager, Player player, long gameTime, long dayTime) {
      this.villager = villager;
      this.player = player;
        this.gameTime = gameTime;
        this.dayTime = dayTime;
    }

   public <T extends DialogueContext> T as() {
      try {
         return (T) this;
      } catch (ClassCastException ex) {
         throw new UnexpectedDialogueContextException(this.getClass());
      }
   }

   public static class UnexpectedDialogueContextException extends RuntimeException {
      public UnexpectedDialogueContextException(Class<? extends DialogueContext> actual) {
         super(
               String.format(
                     "Incorrect type of dialogue context was provided: %s. Make sure that the dialogue screen is passed the right type of dialogue context.",
                     actual));
      }
   }
}
