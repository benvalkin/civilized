package com.uncreated.civilized.core.settlement.family;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.SettlementState;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.villagerinfo.Gender;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.InvalidateImportantLocations;
import com.uncreated.civilized.neoforge.registration.entity.EntityRegistry;
import com.uncreated.civilized.ui.style.Colors;

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
   private static final long BIRTH_TIME_OF_DAY = 18000;

   private final LoadedSettlement loadedSettlement;

   public BirthOrganiser(LoadedSettlement loadedSettlement) {
      this.loadedSettlement = loadedSettlement;
   }

   private Settlement settlement() {
      return loadedSettlement.getSettlement();
   }

   public void serverTick(ServerLevel level, long dayTime) {

      long day = dayTime / 24000;
      SettlementState state = settlement().getState();

      if (state.getLastBirthsCheckDay() > day) // todo: don't really think we to save the last day if it happens daily.
                                               // Let's rather use a daily even scheduler.
         state.setLastBirthsCheckDay(SettlementState.NOT_SCHEDULED);

      // once a night
      if (state.getLastBirthsCheckDay() == day || dayTime % 24000 < BIRTH_TIME_OF_DAY)
         return;

      state.setLastBirthsCheckDay(day);
      ServerSettlementsStore.INSTANCE.setDirty();

      for (Couple couple : findCouplesLivingTogether(dayTime)) {
         if (!hasRoomForAnotherChild(couple.home(), dayTime))
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

   private List<Couple> findCouplesLivingTogether(long dayTime) {

      List<Couple> couples = new ArrayList<>();
      for (VillagerInfo mother : ServerVillagerStore.INSTANCE.getCitizens(settlement().getSettlementId())) {
         if (mother.getGender() != Gender.FEMALE || !isAdultCitizen(mother, dayTime))
            continue;

         Optional<VillagerInfo> father = ServerVillagerStore.INSTANCE.find(mother.getPartnerId());
         if (father.isEmpty() || !isAdultCitizen(father.get(), dayTime))
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

   private static boolean isAdultCitizen(VillagerInfo villager, long dayTime) {
      return villager.getNpcRole().is(VillagerNpcRoles.CITIZEN) && !villager.isChild(dayTime);
   }

   public static int childSlots(Building home) {
      return Math.max(0, home.getUpgradeLevel() - 1);
   }

   private static boolean hasRoomForAnotherChild(Building home, long dayTime) {
      int children = 0;
      for (VillagerInfo resident : BuildingUtil.getOccupants(home, ServerVillagerStore.INSTANCE)) {
         if (resident.isChild(dayTime))
            children++;
      }
      return children < childSlots(home);
   }

   private void tryGiveBirth(ServerLevel level, Couple couple, long day) {

      // the child is spawn at home, which has to be loaded.
      if (LoadedBuildings.checkLoaded(couple.home()).isEmpty())
         return;

      BlockPos birthPos = couple.home().getBounds().findRandomInsideFloorBlock(level);
      CivilizedVillager child =
            EntityRegistry.CIVILIZED_VILLAGER.get().spawn(level, birthPos, EntitySpawnReason.BREEDING);
      if (child == null) {
         LOGGER.error("Tried to spawn new child at {} but something went wrong.", couple.home().getBuildingId());
         return;
      }

      VillagerInfo info = child.getInfo();
      child.changeNpcRole(VillagerNpcRoles.CITIZEN);
      info.setSettlementId(settlement().getSettlementId());
      info.setParents(couple.mother().getVillagerId(), couple.father().getVillagerId());
      info.setBirthDay(day);
      info.setLastName(couple.father().getLastName());
      info.setHomeBuildingId(couple.home().getBuildingId());
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(info, StoreOperation.ADD_OR_OVERWRITE);

      // invalidate to finalize home location and etc
      InvalidateImportantLocations.revalidate(level, child);
      child.refreshBrain(level);

      announceBirth(level, couple, info);
   }

   private void announceBirth(ServerLevel level, Couple couple, VillagerInfo child) {
      Player owner = level.getPlayerByUUID(settlement().getOwnerId());
      if (owner == null)
         return;

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

      owner.displayClientMessage(message.withColor(Colors.LOVE), false);
   }
}
