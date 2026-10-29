package com.uncreated.civilized.core.dialogue.context;

import java.util.UUID;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.quest.QuestType;
import com.uncreated.civilized.core.quest.attachments.PlayerDialogueCooldowns;
import com.uncreated.civilized.core.quest.attachments.PlayerQuests;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.neoforge.registration.attachments.DataAttachments;

import lombok.Getter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Who is talking to whom. */
@Getter
public class DialogueContext {
   private final CivilizedVillager villager;
   private final Player player;
   private final long gameTime;
   private final long dayTime;

   public DialogueContext(CivilizedVillager villager, Player player) {
      this.villager = villager;
      this.player = player;
      this.gameTime = player.level().getGameTime();
      this.dayTime = player.level().getDayTime();
   }

   public ItemStack heldItem() {
      return player.getMainHandItem();
   }

   public PlayerQuests quests() {
      return player.getData(DataAttachments.QUESTS);
   }

   public PlayerDialogueCooldowns cooldowns() {
      return player.getData(DataAttachments.DIALOGUE_COOLDOWNS);
   }

   /** This villager's id for quests given by a specific villager, otherwise null. */
   public @Nullable UUID vendorIdFor(QuestType questType) {
      return questType.isVendorSpecific() ? villager.getInfo().getVillagerId() : null;
   }
}
