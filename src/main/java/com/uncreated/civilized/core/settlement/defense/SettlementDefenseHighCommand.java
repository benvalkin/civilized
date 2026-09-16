package com.uncreated.civilized.core.settlement.defense;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.settlement.entity.LoadedVillagers;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.entity.CivilizedVillager;

import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

@Getter
public class SettlementDefenseHighCommand implements ICombatCommand {

   public static final int DETECTION_INTERVAL_TICKS = 20;

   private final LoadedSettlement loadedSettlement;

   private final List<CivilizedVillager> defenders;
   private final Map<LivingEntity, Long> hostiles;
   private final Map<CivilizedVillager, LivingEntity> targetMap;
   private long nextDetectionTick;

   public SettlementDefenseHighCommand(LoadedSettlement loadedSettlement) {
      this.defenders = new LinkedList<>();
      this.hostiles = new HashMap<>();
      this.targetMap = new HashMap<>();
      this.loadedSettlement = loadedSettlement;
      this.nextDetectionTick = -1;
   }

   public void serverTick(ServerLevel level, long gameTime) {

      if (gameTime <= nextDetectionTick)
         return;

      nextDetectionTick = gameTime + DETECTION_INTERVAL_TICKS;

      findAvailableDefenders(defenders);

      for (CivilizedVillager defender : defenders) {
         if (!hostiles.isEmpty()) {
            defender.draft(this);
         } else {
            defender.undraft();
         }
      }

      hostiles.entrySet().removeIf(e -> !e.getKey().isAlive() || gameTime - e.getValue() > 15 * 20);
   }

   private void findAvailableDefenders(List<CivilizedVillager> defenders) {

      defenders.clear();
      for (CivilizedVillager villager : LoadedVillagers.all()) {

         if (!villager.isAlive())
            continue;

         if (!villager.getInfo().getOccupation().is(VillagerOccupations.SOLDIER))
            continue;

         if (!loadedSettlement.getSettlement().getSettlementId().equals(villager.getInfo().getSettlementId()))
            continue;

         defenders.add(villager);
      }
   }

   @Override
   public Optional<CombatTarget> requestTargetForCombatant(CivilizedVillager villager) {
      Optional<LivingEntity> enemy =
            // closest enemy
            hostiles.keySet()
                  .stream()
                  .min(Comparator.comparingDouble(e -> villager.position().distanceToSqr(e.position())));

      if (enemy.isEmpty())
         return Optional.empty();

      // always sanity check if the enemy is alive as often as possible.
      // it may not have been removed from the enemy yet when this runs
      if (!enemy.get().isAlive())
         return Optional.empty();

      int priority = (int) Math.round(villager.position().distanceToSqr(enemy.get().position()));
      return Optional.of(new CombatTarget(enemy.get(), priority));
   }

   @Override
   public boolean shouldContinueToBeDrafted(CivilizedVillager villager) {
      return defenders.contains(villager);
   }

   @Override
   public boolean shouldContinueToEngageTarget(CivilizedVillager villager, LivingEntity enemy) {
      return hostiles.containsKey(enemy);
   }

   @Override
   public void reportHostile(CivilizedVillager reporter, LivingEntity hostile) {

      if (!loadedSettlement.getSettlement().getBounds().contains(hostile.position()))
         return;

      hostiles.put(hostile, loadedSettlement.getLevel().getGameTime());
   }
}
