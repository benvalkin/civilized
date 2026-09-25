package com.uncreated.civilized.core.dialogue;

import java.util.Random;

import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.network.chat.Component;

public class RandomSpeech {
   public static Component randomOneOf(Component... speechOptions) {
      int randomIndex = new Random().nextInt(0, speechOptions.length);
      return speechOptions[randomIndex];
   }

   public static Component randomPerVillager(CivilizedVillager villager, Component... speechOptions) {
      int randomIndex = villager.getConsistentLifetimeRandom().nextInt(0, speechOptions.length);
      return speechOptions[randomIndex];
   }

   public static Component randomGreeting() {
      return randomOneOf(
            Component.translatable("villager.dialogue.response.misc.greet.1"),
            Component.translatable("villager.dialogue.response.misc.greet.2"),
            Component.translatable("villager.dialogue.response.misc.greet.3"),
            Component.translatable("villager.dialogue.response.misc.greet.4"),
            Component.translatable("villager.dialogue.response.misc.greet.5"));
   }
}
