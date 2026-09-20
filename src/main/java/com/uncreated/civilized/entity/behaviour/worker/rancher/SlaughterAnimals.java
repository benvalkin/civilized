package com.uncreated.civilized.entity.behaviour.worker.rancher;

import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

public class SlaughterAnimals<T extends Animal> extends WorkTaskBehaviour {
   public static final Logger LOGGER = LogUtils.getLogger();
   private long lastWorkTime;
   private MediumDistanceTravelTask travelHelper;

   private final Class<T> animalMobType;

   public SlaughterAnimals(Class<T> animalMobType) {
      super(WorkStates.SLAUGHTERING_ANIMALS, true, true, 120 * 20, 30 * 20);
      this.animalMobType = animalMobType;
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      killableAnimals = getKillableAdultAnimals(level);
      if (killableAnimals.size() <= 4)
         return false;

      return true;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      AABB tooCloseBounds = getWorksite().getBuilding().getBounds().getEncapsulatingAABB();
      AABB closeEnoughBounds = tooCloseBounds.inflate(4);

      travelHelper =
            new MediumDistanceTravelTask(
                  villager,
                  getWorksite().getBuilding().getBlockPos(),
                  (v, d, closEnough) -> closeEnoughBounds.contains(v.position()),
                  Math.max((int) closeEnoughBounds.getXsize() / 2, (int) closeEnoughBounds.getZsize() / 2));

      killedAnimals = false;
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);

      if (killedAnimals)
         goDropOffWorkOutputAtHome(villager);
   }

   private boolean killedAnimals;

   private List<Animal> killableAnimals;

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long gameTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(gameTime);
         return;
      }

      if (gameTime - lastWorkTime > 20 * 3) {

         lastWorkTime = gameTime;

         killableAnimals = getKillableAdultAnimals(level);
         if (killableAnimals.size() <= 4) {
            doStop(level, villager, gameTime);
            return;
         }

         villager.swing(InteractionHand.MAIN_HAND, true);

         Animal toKill = killableAnimals.getFirst();

         toKill.kill(level);

         List<ItemEntity> droppedItems =
               level.getEntitiesOfClass(
                     ItemEntity.class,
                     getWorksite().getBuilding().getBounds().getEncapsulatingAABB());
         droppedItems.forEach(i -> {
            villager.getWorkOutputInventory().addItem(i.getItem());
            i.remove(Entity.RemovalReason.KILLED);
         });

         killedAnimals = true;

      }
   }

   protected List<Animal> getKillableAdultAnimals(ServerLevel level) {
      return level.getEntitiesOfClass(
            Animal.class,
            getWorksite().getBuilding().getBounds().getEncapsulatingAABB(),
            a -> !a.isBaby() && animalMobType.isInstance(a) && !a.isInLove());
   }
}
