package com.uncreated.civilized.core.settlement.defense;

import com.google.common.collect.ImmutableList;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.entity.CivilizedVillager;

import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;

@Getter
public class SettlementThreatDetector {

   public static final int DETECTION_INTERVAL_TICKS = 20;
   private ImmutableList<LivingEntity> hostiles;
   private final LoadedSettlement loadedSettlement;
   private long nextDetectionTick;

   public SettlementThreatDetector(LoadedSettlement loadedSettlement) {
      this.loadedSettlement = loadedSettlement;
      this.nextDetectionTick = -1;
   }

   public void serverTick(ServerLevel level, long gameTime) {
      if (gameTime > nextDetectionTick) {
         nextDetectionTick = gameTime + DETECTION_INTERVAL_TICKS;
         searchForHostiles(level);
      }
   }

   public void searchForHostiles(ServerLevel level) {

      hostiles =
            ImmutableList.copyOf(
                  level.getEntitiesOfClass(
                        LivingEntity.class,
                        loadedSettlement.getBehaviour().getSettlement().getBounds().getEncapsulatingAABB(),
                        this::checkHostile));
   }

   public boolean isThreat(LivingEntity entity) {
      return hostiles.contains(entity);
   }

   private boolean checkHostile(LivingEntity livingEntity) {

      if (livingEntity instanceof CivilizedVillager civilizedVillager) {
         boolean sameTeam =
               loadedSettlement.getSettlement().getSettlementId().equals(civilizedVillager.getInfo().getSettlementId());
         if (sameTeam)
            return false;
      }

      if (livingEntity instanceof Monster && !(livingEntity instanceof Creeper)) {
         return true;
      }

      return false;
   }
}
