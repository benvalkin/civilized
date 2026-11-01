package com.uncreated.civilized.entity;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

public abstract class VillagerRoleBehaviour {

   @Getter
   protected final CivilizedVillager villager;

   public VillagerRoleBehaviour(CivilizedVillager villager) {
      this.villager = villager;
   }

   public static final String FIELD_ROLE_BEHAVIOUR_STATE = "role_behaviour_state";

   public abstract void addAdditionalSaveData(CompoundTag compound);

   public abstract void readAdditionalSaveData(CompoundTag compound);

   public abstract void serverTick(ServerLevel level, long gameTime);
}
