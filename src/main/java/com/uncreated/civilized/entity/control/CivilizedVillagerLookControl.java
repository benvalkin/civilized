package com.uncreated.civilized.entity.control;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;

/**
 * A LookControl for the CivilizedVillager. It's exactly the same as a regular look control, except it doesn't work
 * while sleeping. It was added to prevent villagers trying to look around while sleeping.
 */
public class CivilizedVillagerLookControl extends LookControl {

   public CivilizedVillagerLookControl(Mob mob) {
      super(mob);
   }

   @Override
   public void tick() {
      if (!mob.isSleeping()) {
         super.tick();
         return;
      }

      // when going to sleep, override to look pos to look forward instead of the look target
      lookAtCooldown = 0;
      mob.yHeadRot = mob.yBodyRot;
      mob.setXRot(0.0F);
   }
}
