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

   public void serverTick(ServerLevel level, long gameTime) {
      // base must be empty otherwise all subclasses need to call this super method
   }
}
