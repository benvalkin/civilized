package com.uncreated.civilized.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

public class DefaultRoleBehaviour extends VillagerRoleBehaviour {
   public DefaultRoleBehaviour(CivilizedVillager villager) {
      super(villager);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag compound) {

   }

   @Override
   public void readAdditionalSaveData(CompoundTag compound) {

   }

   @Override
   public void serverTick(ServerLevel level, long gameTime) {

   }
}
