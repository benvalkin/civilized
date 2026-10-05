package com.uncreated.civilized.core.settlement.entity.behaviour;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.logistics.LogisticsManager;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.defense.SettlementDefenseHighCommand;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.settlement.tax.TaxCollector;

import lombok.Getter;
import net.minecraft.server.level.ServerLevel;

public class SettlementBehaviour {

   protected final Logger LOGGER = LogUtils.getLogger();

   @Getter
   private final LogisticsManager logisticsManager;

   @Getter
   private final SettlementDefenseHighCommand defenseHighCommand;

   @Getter
   private final TaxCollector taxCollector;

   @Getter
   private final LoadedSettlement entity;

   public Settlement getSettlement() {
      return entity.getSettlement();
   }

   public SettlementBehaviour(LoadedSettlement entity) {
      this.entity = entity;
      logisticsManager = new LogisticsManager();
      defenseHighCommand = new SettlementDefenseHighCommand(entity);
      taxCollector = new TaxCollector(entity);
   }

   public void serverTick(ServerLevel level, long gameTime, long dayTime) {
      defenseHighCommand.serverTick(level, gameTime);
      taxCollector.serverTick(level, gameTime, dayTime);
   }
}
