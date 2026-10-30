package com.uncreated.civilized.entity;

import java.util.Optional;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.skills.SkillEffects;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkill;

import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

public class VisitorBehaviour extends VillagerRoleBehaviour {

   public static final String FIELD_DEPART_AT = "depart_at";
   public static final long NO_DEPARTURE = -1;
   /** No visitor stays longer than this, so a departure further away means the clock was set back. */
   public static final long MAX_STAY_TICKS = 4 * 24000;
   private static final float HOSPITALITY_PER_CHAT = 3;

   @Setter
   private long departAt = NO_DEPARTURE;
   @Setter
   private boolean departurePaused;

   public VisitorBehaviour(CivilizedVillager villager) {
      super(villager);
   }

   public static Optional<VisitorBehaviour> of(CivilizedVillager villager) {
      if (villager.getRoleBehaviour() instanceof VisitorBehaviour visitor)
         return Optional.of(visitor);

      return Optional.empty();
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
      return dayTime >= departAt || departAt - dayTime > MAX_STAY_TICKS;
   }

   public void gainHospitalityFromChatting(CivilizedVillager citizen) {
      int socialLevel = citizen.getInfo().getSkills().getLevel(VillagerSkill.SOCIAL);
      float amount = HOSPITALITY_PER_CHAT * SkillEffects.getHospitalityMultiplier(socialLevel) * warmth();
      villager.getInfo().getHospitality().gainFromChatting(amount, villager.level().getDayTime() / 24000);
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(villager.getInfo(), StoreOperation.UPDATE);
   }

   /** Some visitors warm to a settlement quicker than others: x0.7 to x1.3, the same for the whole visit. */
   private float warmth() {
      return 0.7f + villager.perLifetimeRandom(2).nextFloat() * 0.6f;
   }
}
