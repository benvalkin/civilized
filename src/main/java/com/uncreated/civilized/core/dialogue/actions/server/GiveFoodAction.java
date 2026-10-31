package com.uncreated.civilized.core.dialogue.actions.server;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.recruitment.VisitorHospitality;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.VisitorBehaviour;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public class GiveFoodAction implements IServerDialogueAction {

   private final int minimumFood;

   public GiveFoodAction(int minimumFood) {
      this.minimumFood = minimumFood;
   }

   @Override
   public boolean run(ServerPlayer player, CivilizedVillager villager) {
      ItemStack held = player.getMainHandItem();
      FoodProperties food = held.get(DataComponents.FOOD);
      if (food == null || food.nutrition() < minimumFood)
         return false;

      // visitors only take one meal a visit, which counts towards their hospitality
      boolean isVisitor = VisitorBehaviour.tryGet(villager).isPresent();
      if (isVisitor && !villager.getInfo().getHospitality().feed(VisitorHospitality.FED_BONUS))
         return false;

      held.consume(1, player);
      villager.level()
            .playSound(
                  null,
                  villager.getX(),
                  villager.getY(),
                  villager.getZ(),
                  SoundEvents.PLAYER_BURP,
                  SoundSource.NEUTRAL,
                  1,
                  1);
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(villager.getInfo(), StoreOperation.UPDATE);
      return true;
   }
}
