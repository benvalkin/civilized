package com.uncreated.civilized.entity.behaviour;

import lombok.EqualsAndHashCode;
import net.minecraft.network.chat.Component;

@EqualsAndHashCode(callSuper = false)
public class BehaviourState {

   private final String value;

   public BehaviourState(String value) {
      this.value = value;
   }

   public boolean is(BehaviourState other) {
      return this.value.equals(other.value);
   }

   @Override
   public String toString() {
      return value;
   }

   public Component description() {
      return Component.translatableWithFallback("villager.behaviour." + value + ".description", "");
   }
}
