package com.uncreated.civilized.core.settlement.entity;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.settlement.Settlement;

import com.uncreated.civilized.core.settlement.entity.behaviour.SettlementBehaviour;
import lombok.Getter;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

@Getter
public class LoadedSettlement {
   private final Settlement settlement;
   private final Level level;
   private final SettlementBehaviour behaviour;
   private final Set<LoadedBuilding> loadedBuildings;

   void add(LoadedBuilding building) {
      loadedBuildings.add(building);
   }
   void remove(LoadedBuilding building) {
      loadedBuildings.remove(building);
   }

   public LoadedSettlement(Settlement settlement, Level level) {
      this.settlement = settlement;
      this.level = level;
       this.loadedBuildings = new HashSet<>();
       behaviour = new SettlementBehaviour(this);
   }
}
