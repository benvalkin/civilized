package com.uncreated.civilized.entity.data;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.entity.CivilizedVillager;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

@Accessors(fluent = true)
public class VillagerHunger {
   public static final float WALK_DISTANCE_PER_EXHAUSTION_STEP = 3F;
   public static final int HUNGRY_THRESHOLD = 14;

   private static final String FIELD_HUNGER = "hunger";
   private static final String FIELD_SATURATION = "saturation";
   private static final String FIELD_EXHAUSTION = "exhaustion";
   private static final String FIELD_HUNGRY_START_TIME = "hungry_start_time";
   public static final int EXHAUSTION_PER_FOOD_POINT = 4;
   public static final float EXHAUSTION_PER_WALKING_STEP = 0.1f;
   private final CivilizedVillager villager;
   @Nullable
   private Vec3 lastPos;
   @Getter
   @Setter
   private float exhaustion;

   public VillagerHunger(CivilizedVillager villager) {
      this.villager = villager;
   }

   public int saturation() {
      return villager.getEntityData().get(CivilizedVillager.SATURATION);
   }

   public int hunger() {
      return villager.getEntityData().get(CivilizedVillager.HUNGER);
   }

   public boolean isHungry() {
      return hunger() <= HUNGRY_THRESHOLD;
   }

   public long timeSpentHungry() {
      long hungryStartTime = villager.getEntityData().get(CivilizedVillager.HUNGRY_START_TIME);
      if (hungryStartTime == -1)
         return 0;
      return villager.level().getGameTime() - hungryStartTime;
   }

   public int daysSpentHungry() {
      return (int) (timeSpentHungry() / 24000);
   }

   public void save(CompoundTag tag) {
      tag.putInt(FIELD_HUNGER, hunger());
      tag.putInt(FIELD_SATURATION, saturation());
      tag.putFloat(FIELD_EXHAUSTION, exhaustion);
      tag.putLong(FIELD_HUNGRY_START_TIME, villager.getEntityData().get(CivilizedVillager.HUNGRY_START_TIME));
   }

   public void load(CompoundTag tag) {
      if (!tag.contains(FIELD_HUNGER))
         return;

      villager.getEntityData().set(CivilizedVillager.HUNGER, tag.getInt(FIELD_HUNGER));
      villager.getEntityData().set(CivilizedVillager.SATURATION, tag.getInt(FIELD_SATURATION));
      exhaustion = tag.getFloat(FIELD_EXHAUSTION);
      villager.getEntityData().set(CivilizedVillager.HUNGRY_START_TIME, tag.getLong(FIELD_HUNGRY_START_TIME));
   }

   public void addExhaustion(float exhaustionToAdd) {
      this.exhaustion += exhaustionToAdd;
   }

   public void serverTickHunger(long gameTime) {

      int hunger = villager.getEntityData().get(CivilizedVillager.HUNGER);
      int saturation = villager.getEntityData().get(CivilizedVillager.SATURATION);
      long hungryStartTime = villager.getEntityData().get(CivilizedVillager.HUNGRY_START_TIME);
      int oldhunger = hunger;
      int oldsaturation = saturation;

      if (lastPos == null)
         lastPos = villager.position();

      if (lastPos.distanceToSqr(villager.position()) >= WALK_DISTANCE_PER_EXHAUSTION_STEP
            * WALK_DISTANCE_PER_EXHAUSTION_STEP) {
         // POTENTIALLY BAD IMPLEMENTATION: this path also fires when the villager is nudged/teleported.
         lastPos = villager.position();
         exhaustion += EXHAUSTION_PER_WALKING_STEP;
      }

      if (exhaustion >= EXHAUSTION_PER_FOOD_POINT) {
         exhaustion = exhaustion - EXHAUSTION_PER_FOOD_POINT;

         // only decrease either saturation or hunger
         if (saturation > 0)
            saturation = saturation - 1;
         else
            hunger = Math.max(hunger - 1, 0);
      }

      if (isHungry()) {
         // POTENTIALLY BAD IMPLEMENTATION: game time different approach does not account for when the villager is
         // unloaded
         if (hungryStartTime == -1)
            villager.getEntityData().set(CivilizedVillager.HUNGRY_START_TIME, gameTime);
      } else if (hungryStartTime != -1) {
         villager.getEntityData().set(CivilizedVillager.HUNGRY_START_TIME, -1L);
      }

      if (saturation != oldsaturation)
         villager.getEntityData().set(CivilizedVillager.SATURATION, saturation);
      if (hunger != oldhunger)
         villager.getEntityData().set(CivilizedVillager.HUNGER, hunger);
   }
}
