package com.uncreated.civilized.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

public abstract class VillagerState {

   protected final CivilizedVillager villager;

   public VillagerState(CivilizedVillager villager) {
      this.villager = villager;
   }

   public static final String FIELD_VILLAGER_STATE = "villager_state";

   public abstract void addAdditionalSaveData(CompoundTag compound);

   public abstract void readAdditionalSaveData(CompoundTag compound);

   public abstract void serverTick(ServerLevel level, long gameTime);
}
