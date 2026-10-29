package com.uncreated.civilized.core.dialogue;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;

/** Whether a choice is shown and whether it can be picked. */
public record ChoiceState(boolean isVisible, boolean isEnabled, @Nullable Component tooltip) {

   public static ChoiceState enabled() {
      return new ChoiceState(true, true, null);
   }

   public static ChoiceState enabled(Component tooltip) {
      return new ChoiceState(true, true, tooltip);
   }

   public static ChoiceState disabled(Component tooltip) {
      return new ChoiceState(true, false, tooltip);
   }

   public static ChoiceState hidden() {
      return new ChoiceState(false, false, null);
   }
}
