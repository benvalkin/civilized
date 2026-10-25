package com.uncreated.civilized.core.settlement.family;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.settlement.entity.LoadedVillagers;
import com.uncreated.civilized.core.villagerinfo.Gender;
import com.uncreated.civilized.core.villagerinfo.LifeStage;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkills;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.InvalidateImportantLocations;
import com.uncreated.civilized.neoforge.registration.entity.EntityRegistry;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.util.random.DailyEventScheduler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;

public class BirthOrganiser {

   private static final Logger LOGGER = LogUtils.getLogger();

   public static final int BIRTH_ODDS_PERCENT = 6;
   public static final int BIRTH_ODDS_PERCENT_HOMEMAKER = 12;
   private static final int BIRTH_WINDOW_START_TIME = 21000;
   private static final int BIRTH_WINDOW_END_TIME = 23000;

   private final LoadedSettlement loadedSettlement;
   /** Once a night, at a random time, every couple gets a chance of having a child. */
   private final DailyEventScheduler birthScheduler =
         new DailyEventScheduler(1, BIRTH_WINDOW_START_TIME, BIRTH_WINDOW_END_TIME, 1F);

   public BirthOrganiser(LoadedSettlement loadedSettlement) {
      this.loadedSettlement = loadedSettlement;
   }

   private Settlement settlement() {
      return loadedSettlement.getSettlement();
   }

   public void serverTick(ServerLevel level, long gameTime, long dayTime) {
      birthScheduler.tick(level, gameTime, () -> {
         growUpChildren(level, dayTime);
         rollBirths(level, dayTime);
      });
   }

   private void growUpChildren(ServerLevel level, long dayTime) {
      List<VillagerInfo> grownUp = new ArrayList<>();
      for (VillagerInfo villager : ServerVillagerStore.INSTANCE.getCitizens(settlement().getSettlementId())) {
         if (villager.isOldEnoughToGrowUp(dayTime))
            grownUp.add(villager);
      }

      for (VillagerInfo villager : grownUp) {
         villager.setLifeStage(LifeStage.ADULT);
         ServerVillagerStore.INSTANCE.replicateChange(villager, StoreOperation.UPDATE);

         // e.g. refresh their appearance
         Optional<CivilizedVillager> entity = LoadedVillagers.find(villager.getVillagerId());
         if (entity.isPresent()) {
            entity.get().refreshBrain(level);
            entity.get().updateClothing();
         }

         announce(
               level,
               Component.translatable("message.notification.villager.grown_up", villager.getFullName())
                     .withColor(Colors.FAMILY));
      }

      if (!grownUp.isEmpty())
         ServerVillagerStore.INSTANCE.setDirty();
   }

   private void rollBirths(ServerLevel level, long dayTime) {
      long day = dayTime / 24000;
      for (Couple couple : findCouplesLivingTogether()) {
         if (!hasRoomForAnotherChild(couple.home()))
            continue;

         int odds = couple.hasHomemaker() ? BIRTH_ODDS_PERCENT_HOMEMAKER : BIRTH_ODDS_PERCENT;
         if (level.getRandom().nextInt(100) < odds)
            tryGiveBirth(level, couple, day);
      }
   }

   private record Couple(VillagerInfo mother, VillagerInfo father, Building home) {

      boolean hasHomemaker() {
         return mother.isHomemaker() || father.isHomemaker();
      }
   }

   private List<Couple> findCouplesLivingTogether() {

      List<Couple> couples = new ArrayList<>();
      for (VillagerInfo mother : ServerVillagerStore.INSTANCE.getCitizens(settlement().getSettlementId())) {
         if (mother.getGender() != Gender.FEMALE || !isAdultCitizen(mother))
            continue;

         Optional<VillagerInfo> father = ServerVillagerStore.INSTANCE.find(mother.getPartnerId());
         if (father.isEmpty() || !isAdultCitizen(father.get()))
            continue;

         boolean parentsLiveTogether =
               mother.getHomeBuildingId() == null
                     || !mother.getHomeBuildingId().equals(father.get().getHomeBuildingId());
         if (parentsLiveTogether)
            continue;

         Optional<Building> home = ServerBuildingsStore.INSTANCE.find(mother.getHomeBuildingId());
         if (home.isEmpty() || !home.get().getBuildingType().isFamilyResidence())
            continue;

         couples.add(new Couple(mother, father.get(), home.get()));
      }
      return couples;
   }

   private static boolean isAdultCitizen(VillagerInfo villager) {
      return villager.getNpcRole().is(VillagerNpcRoles.CITIZEN) && !villager.isChild();
   }

   public static int childSlots(Building home) {
      return Math.max(0, home.getUpgradeLevel() - 1);
   }

   private static boolean hasRoomForAnotherChild(Building home) {
      int children = 0;
      for (VillagerInfo resident : BuildingUtil.getOccupants(home, ServerVillagerStore.INSTANCE)) {
         if (resident.isChild())
            children++;
      }
      return children < childSlots(home);
   }

   private void tryGiveBirth(ServerLevel level, Couple couple, long day) {

      // the child is spawn at home, which has to be loaded.
      if (LoadedBuildings.checkLoaded(couple.home()).isEmpty())
         return;

      Optional<CivilizedVillager> child =
            spawnChild(level, couple.home(), couple.mother(), couple.father(), day);
      child.ifPresent(c -> announceBirth(level, couple, c.getInfo()));
   }

   public static Optional<CivilizedVillager> spawnChild(
         ServerLevel level,
         Building home,
         @Nullable VillagerInfo mother,
         @Nullable VillagerInfo father,
         long birthDay) {

      BlockPos birthPos = home.getBounds().findRandomInsideFloorBlock(level);
      CivilizedVillager child = EntityRegistry.CIVILIZED_VILLAGER.get().spawn(level, birthPos, EntitySpawnReason.BREEDING);
      if (child == null) {
         LOGGER.error("Tried to spawn new child at {} but something went wrong.", home.getBuildingId());
         return Optional.empty();
      }

      VillagerInfo info = child.getInfo();
      child.changeNpcRole(VillagerNpcRoles.CITIZEN);
      info.setSettlementId(home.getSettlementId());
      if (mother != null && father != null) {
         info.setParents(mother.getVillagerId(), father.getVillagerId());
         info.setLastName(father.getLastName());
      }
      info.setBirthDay(birthDay);
      info.setLifeStage(LifeStage.CHILD);
      // newly spawned villagers start with some skills, but newborns haven't learnt anything yet
      info.setSkills(VillagerSkills.allSkillsZeroXp());
      info.setHomeBuildingId(home.getBuildingId());
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(info, StoreOperation.ADD_OR_OVERWRITE);

      // invalidate to finalize home location and etc
      InvalidateImportantLocations.revalidate(level, child);
      child.refreshBrain(level);
      return Optional.of(child);
   }

   private void announceBirth(ServerLevel level, Couple couple, VillagerInfo child) {
      MutableComponent message;
      if (child.getGender() == Gender.MALE)
         message =
               Component.translatable(
                     "message.notification.villager.child_born.male",
                     couple.father().getLastName(),
                     child.getFirstName());
      else
         message =
               Component.translatable(
                     "message.notification.villager.child_born.female",
                     couple.father().getLastName(),
                     child.getFirstName());

      announce(level, message);
   }

   private void announce(ServerLevel level, MutableComponent message) {
      Player owner = level.getPlayerByUUID(settlement().getOwnerId());
      if (owner != null)
         owner.displayClientMessage(message.withColor(Colors.FAMILY), false);
   }
}
