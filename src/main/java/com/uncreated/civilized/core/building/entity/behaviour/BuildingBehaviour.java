package com.uncreated.civilized.core.building.entity.behaviour;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;

import lombok.Getter;
import net.minecraft.server.level.ServerLevel;

public class BuildingBehaviour {

   protected final Logger LOGGER = LogUtils.getLogger();

   @Getter
   private final LoadedBuilding entity;

   public Building getBuilding() {
      return entity.getBuilding();
   }

   public BuildingBehaviour(LoadedBuilding entity) {
      this.entity = entity;
   }

   public void start() {

   }

   private long lastResetDay = -1;

   public final void serverTickInternal(ServerLevel level, long gameTime, long dayTime) {
      long day = dayTime / 24000L;
      if (day != lastResetDay) {
         lastResetDay = day;
         getEntity().cancelAllReservations();
      }
      try {
         serverTick(level, gameTime, dayTime);
      } catch (Exception ex) {
         LOGGER.error("Error while ticking building {}", getBuilding().getBuildingId(), ex);
      }
   }

   public void serverTick(ServerLevel level, long gameTime, long dayTime) {
      // base must be empty
   }

}
