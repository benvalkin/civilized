package com.uncreated.civilized.entity.behaviour.social;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.VisitorBehaviour;
import com.uncreated.civilized.entity.behaviour.BehaviourStates;
import com.uncreated.civilized.entity.behaviour.Cooldowns;

import net.minecraft.server.level.ServerLevel;

public class EntertainGuests extends Socialize {

   private static final Duration COOLDOWN = Duration.of(30, ChronoUnit.SECONDS);

   public EntertainGuests(float speedModifier) {
      super(BehaviourStates.ENTERTAINING_GUESTS, speedModifier, 20 * 20, 40 * 20, 0);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      // only tavern keepers can run this behaviour
      if (!villager.getInfo().getOccupation().is(VillagerOccupations.TAVERN_KEEPER))
         return false;

      if (conversationFromMemory(villager).isPresent())
         return false;

      if (getBehaviourCooldowns().hasCooldown(Cooldowns.START, level.getGameTime()))
         return false;

      return tryStartConversation(level, villager);
   }

   @Override
   protected @NotNull List<CivilizedVillager> getCandidateConversationMembers(
         ServerLevel level,
         CivilizedVillager villager) {
      Optional<Building> tavern = ServerBuildingsStore.INSTANCE.find(villager.getInfo().getWorkBuildingId());
      if (tavern.isEmpty() || !isInsideTavern(villager, tavern.get()))
         return List.of();

      UUID settlementId = villager.getInfo().getSettlementId();
      return level
            .getEntitiesOfClass(
                  CivilizedVillager.class,
                  tavern.get().getBounds().getEncapsulatingAABB(),
                  other -> other != villager && other.isAlive() && !other.isDrafted()
                        && isInsideTavern(other, tavern.get()) && isGuest(other, settlementId))
            .stream()
            .sorted(Comparator.comparingDouble(villager::distanceToSqr))
            .toList();
   }

   private boolean isInsideTavern(CivilizedVillager villager, Building building) {
      return building.getBounds().contains(villager.blockPosition());
   }

   private boolean isGuest(CivilizedVillager guest, UUID settlementId) {
      return settlementId != null && (settlementId.equals(guest.getInfo().getSettlementId())
            || settlementId.equals(guest.getInfo().getVisitingSettlementId()));
   }

   @Override
   protected void giveSocializationRewardsToVisitor(VisitorBehaviour visitorBehaviour, CivilizedVillager tavernKeeper) {
      visitorBehaviour.gainHospitalityFromEntertainment(tavernKeeper);
      tavernKeeper.getSkillTraining().gainHostingXp();
   }

   @Override
   protected void giveSocializationRewardsToCitizen(CivilizedVillager chatted, CivilizedVillager hospitalityHost) {
      // todo: give mood buff
      hospitalityHost.getSkillTraining().gainHostingXp();
   }

   @Override
   protected Duration getRestartCooldown() {
      return COOLDOWN;
   }
}
