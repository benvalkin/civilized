package com.uncreated.civilized.core.quest.attachments;

import java.time.Duration;

import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.util.CooldownTracker;

/**
 * A simple in-memory DataAttachment to save dialogue cooldowns to prevent certain repetitive mundane dialogues from appearing
 * on every interaction. Dialogue cooldowns are not persisted and are not intended to be copied on death. Dialogue
 * cooldowns are NOT intended to be used for PlayQuest availability.
 */
public class PlayerDialogueCooldowns {
   private final CooldownTracker<String> cooldownTracker;

   public PlayerDialogueCooldowns() {
      cooldownTracker = new CooldownTracker<>();
   }

   public boolean hasCooldown(CivilizedVillager villager, String dialogueKey, long gameTime) {
      return cooldownTracker.hasCooldown(keyFor(villager, dialogueKey), gameTime);
   }

   public void startCooldown(CivilizedVillager villager, String dialogueKey, Duration duration, long gameTime) {
      cooldownTracker.startCooldown(keyFor(villager, dialogueKey), duration, gameTime);
   }

   private static String keyFor(CivilizedVillager villager, String dialogueKey) {
      return villager.getVillagerId().toString() + "_" + dialogueKey;
   }
}
