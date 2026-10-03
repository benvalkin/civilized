package com.uncreated.civilized.entity;

import java.util.Optional;

import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

public class VisitorState extends VillagerState {

   public static final String FIELD_DEPART_AT = "depart_at";
   public static final long NO_DEPARTURE = -1;

   @Setter
   private long departAt = NO_DEPARTURE;
   @Setter
   private boolean departurePaused;

   public VisitorState(CivilizedVillager villager) {
      super(villager);
   }

   public static Optional<VisitorState> of(CivilizedVillager villager) {
      return villager.getState() instanceof VisitorState visitor ? Optional.of(visitor) : Optional.empty();
   }

   @Override
   public void addAdditionalSaveData(CompoundTag compound) {
      compound.putLong(FIELD_DEPART_AT, departAt);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag compound) {
      if (compound.contains(FIELD_DEPART_AT))
         departAt = compound.getLong(FIELD_DEPART_AT);
   }

   @Override
   public void serverTick(ServerLevel level, long gameTime) {
      if (isTimeToDepart(level.getDayTime()))
         villager.depart();
   }

   private boolean isTimeToDepart(long dayTime) {
      if (departAt == NO_DEPARTURE || departurePaused)
         return false;

      // the second check catches the clock being set back (e.g. with /time set), which would otherwise leave the
      // visitor waiting for days
      return dayTime >= departAt || departAt - dayTime > 24000;
   }
}
