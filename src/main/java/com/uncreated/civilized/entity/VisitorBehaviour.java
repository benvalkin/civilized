package com.uncreated.civilized.entity;

import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.building.entity.behaviour.InnBehaviour;
import com.uncreated.civilized.core.recruitment.ImpressionRequirement;
import com.uncreated.civilized.core.recruitment.VisitorHospitality;
import com.uncreated.civilized.core.recruitment.WillingToJoinNotification;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.inactive.visitor.ServerRecurringVisitorStore;
import com.uncreated.civilized.core.villagerinfo.skills.SkillEffects;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkill;
import com.uncreated.civilized.networking.packets.VisitorHospitalityUpdated;
import com.uncreated.civilized.ui.menu.villager.VillagerMenu;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class VisitorBehaviour extends VillagerRoleBehaviour {

   public static final String FIELD_DEPART_AT = "depart_at";
   private static final String FIELD_WILLING_TO_JOIN = "willing_to_join";
   private static final String FIELD_HOSPITALITY = "hospitality";
   private static final String FIELD_NUMBER_OF_VISITS = "number_of_visits";
   private static final String FIELD_VISIT_START_TIME = "visit_start_time";
   public static final long NO_DEPARTURE = -1;
   public static final long MAX_STAY_TICKS = 4 * 24000;
   private static final float HOSPITALITY_PER_CHAT = 1;
   private static final float HOSPITALITY_PER_HOSTED_CHAT = 2.5f;
   private static final float HOSPITALITY_PER_ENTERTAINMENT = 5;
   private static final int NOTIFICATION_CHECK_INTERVAL = 10 * 20;
   private static final float AVG_DAILY_HOSPITALITY_TO_RETURN = 0.8f * VisitorHospitality.DAILY_CAP;
   public static final float DAY_VISITOR_BASE_RETURN_CHANCE = 0.5f;
   public static final float HOSPITALITY_RETAINED_AFTER_RETURNING = 0.6f;

   @Getter
   private long visitStartTime;
   @Setter
   private long departAt = NO_DEPARTURE;
   @Setter
   private boolean departurePaused;
   private boolean willingToJoin;
   @Getter
   private final VisitorHospitality hospitality;
   @Getter
   private int numberOfVisits = 1;

   public VisitorBehaviour(CivilizedVillager villager) {
      super(villager);
      this.hospitality = new VisitorHospitality();
   }

   public static Optional<VisitorBehaviour> tryGet(CivilizedVillager villager) {
      if (villager.getRoleBehaviour() instanceof VisitorBehaviour visitor)
         return Optional.of(visitor);

      return Optional.empty();
   }

   @Override
   public void addAdditionalSaveData(CompoundTag compound) {
      compound.putLong(FIELD_VISIT_START_TIME, visitStartTime);
      compound.putLong(FIELD_DEPART_AT, departAt);
      compound.putBoolean(FIELD_WILLING_TO_JOIN, willingToJoin);
      compound.put(FIELD_HOSPITALITY, hospitality.toNbt());
      compound.putInt(FIELD_NUMBER_OF_VISITS, numberOfVisits);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag compound) {
      if (compound.contains(FIELD_VISIT_START_TIME))
         visitStartTime = compound.getLong(FIELD_VISIT_START_TIME);
      if (compound.contains(FIELD_DEPART_AT))
         departAt = compound.getLong(FIELD_DEPART_AT);
      willingToJoin = compound.getBoolean(FIELD_WILLING_TO_JOIN);
      if (compound.contains(FIELD_HOSPITALITY))
         hospitality.copyFrom(VisitorHospitality.fromNbt(compound.getCompound(FIELD_HOSPITALITY)));
      if (compound.contains(FIELD_NUMBER_OF_VISITS))
         numberOfVisits = compound.getInt(FIELD_NUMBER_OF_VISITS);
   }

   @Override
   public void serverTick(ServerLevel level, long gameTime) {
      if (isTimeToDepart(level.getDayTime())) {
         // also withdrawn when they check into an inn, so that it can be sent again saying where to find them
         WillingToJoinNotification.withdrawNotification(villager.getInfo());
         willingToJoin = false;

         if (!tryCheckIntoInn(level))
            departSettlement(level);
         return;
      }

      // +getId() is just so that visitors arriving together don't all check on the same tick (staggers the
      // notification)
      if ((gameTime + villager.getId()) % NOTIFICATION_CHECK_INTERVAL == 0)
         willingToJoin = WillingToJoinNotification.trySendNotification(villager.getInfo(), hospitality, willingToJoin);
   }

   private void departSettlement(ServerLevel level) {
      VillagerInfo info = villager.getInfo();
      villager.depart();
      if (shouldReturn(info, level.getGameTime())) {
         info.setHomeBuildingId(null);
         info.setPartnerId(null);
         ServerRecurringVisitorStore.INSTANCE.remember(info, hospitality, numberOfVisits, level.getDayTime() / 24000);
      }
   }

   public boolean shouldReturn(VillagerInfo visitor, long gameTime) {
      ImpressionRequirement tier = visitor.getNpcRole().recruitTier();
      if (!(tier == ImpressionRequirement.MID || tier == ImpressionRequirement.HIGH))
         return false;

      UUID settlementId = visitor.getVisitingSettlementId();
      if (settlementId == null || ServerSettlementsStore.INSTANCE.find(settlementId).isEmpty())
         return false;

      if (villager.getInfo().getHomeBuildingId() == null
            && villager.getRandom().nextFloat() < DAY_VISITOR_BASE_RETURN_CHANCE)
         return false; // day visitors do not usually return

      float stayedForDays = getVisitDuration(gameTime) / 24000f;
      float avgHospitalityPerDay = hospitality.getGainedDuringVisit() / Math.max(stayedForDays, 1);
      return avgHospitalityPerDay >= AVG_DAILY_HOSPITALITY_TO_RETURN;
   }

   public long getVisitDuration(long gameTime) {
      return gameTime - visitStartTime;
   }

   public void resetPerVisitStats(long gameTime) {
      hospitality.resetPerVisitPoints();
      visitStartTime = gameTime;
   }

   public void restoreFromPreviousVisit(int previousVisits, float previousHospitality) {
      numberOfVisits = previousVisits + 1;
      hospitality.restorePoints(previousHospitality * HOSPITALITY_RETAINED_AFTER_RETURNING);
      hospitalityChanged();
   }

   private boolean tryCheckIntoInn(ServerLevel level) {
      boolean alreadyStaying = villager.getInfo().getHomeBuildingId() != null;
      UUID visitingSettlementId = villager.getInfo().getVisitingSettlementId();
      if (alreadyStaying || visitingSettlementId == null)
         return false;

      Optional<InnBehaviour> inn = InnBehaviour.findAvailableInn(visitingSettlementId);
      return inn.isPresent() && inn.get().tryCheckIn(level, villager);
   }

   private boolean isTimeToDepart(long dayTime) {
      if (departAt == NO_DEPARTURE || departurePaused)
         return false;

      // the second check catches the clock being set back (e.g. with /time set), which would otherwise leave the
      // visitor waiting for days
      return dayTime >= departAt || departAt - dayTime > MAX_STAY_TICKS;
   }

   public void gainHospitalityFromChatting(CivilizedVillager citizen) {
      gainHospitality(HOSPITALITY_PER_CHAT, citizen.getInfo());
   }

   public void gainHospitalityFromHosting(CivilizedVillager citizen) {
      gainHospitality(HOSPITALITY_PER_HOSTED_CHAT, citizen.getInfo());
   }

   public void gainHospitalityFromEntertainment(CivilizedVillager tavernKeeper) {
      gainHospitality(HOSPITALITY_PER_ENTERTAINMENT, tavernKeeper.getInfo());
   }

   public void gainHospitalityFromFestival() {
      hospitality.awardUncapped(VisitorHospitality.FESTIVAL_BONUS);
      hospitalityChanged();
   }

   /** @return false if they had already been welcomed this visit */
   public boolean welcome() {
      if (!hospitality.welcome(VisitorHospitality.WELCOME_BONUS))
         return false;

      hospitalityChanged();
      return true;
   }

   /** @return false if they had already been fed this visit */
   public boolean feed() {
      if (!hospitality.feed(VisitorHospitality.FED_BONUS))
         return false;

      hospitalityChanged();
      return true;
   }

   private void gainHospitality(float baseAmount, VillagerInfo host) {
      int socialLevel = host.getSkills().getLevel(VillagerSkill.SOCIAL);
      float amount = baseAmount * SkillEffects.getHospitalityMultiplier(socialLevel);
      hospitality.award(amount, villager.level().getDayTime() / 24000);
      hospitalityChanged();
   }

   private void hospitalityChanged() {
      if (!(villager.level() instanceof ServerLevel level))
         return;

      VisitorHospitalityUpdated packet = new VisitorHospitalityUpdated(villager.getId(), hospitality);
      for (ServerPlayer player : level.players()) {
         if (player.containerMenu instanceof VillagerMenu menu && menu.getVillager() == villager)
            PacketDistributor.sendToPlayer(player, packet);
      }
   }
}
