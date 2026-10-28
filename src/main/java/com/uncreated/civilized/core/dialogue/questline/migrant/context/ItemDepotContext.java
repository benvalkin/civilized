package com.uncreated.civilized.core.dialogue.questline.migrant.context;

import java.util.function.Predicate;

import com.uncreated.civilized.core.dialogue.context.DialogueContext;
import com.uncreated.civilized.entity.CivilizedVillager;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class ItemDepotContext extends DialogueContext {

   private final ItemStack heldItem;
   private int amountToConsume = 1;
   @Nullable
   private SoundEvent consumeSound;

   public ItemDepotContext(
         CivilizedVillager villager,
         Player player,
         long gameTime) {
      super(villager, player, gameTime, gameTime);

      heldItem = player.getItemBySlot(EquipmentSlot.MAINHAND);
   }
}
