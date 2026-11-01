package com.uncreated.civilized.core.building.entity.behaviour;

import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.recruitment.ImpressionRequirement;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.VisitorBehaviour;
import com.uncreated.civilized.item.CurrencyItem;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class InnBehaviour extends BuildingBehaviour {

   public static final int CURRENCY_RENT_PER_NIGHT = 5;

   public InnBehaviour(LoadedBuilding entity) {
      super(entity);
   }

   private static final int MIN_STAY_DAYS = 1;
   private static final int MAX_STAY_DAYS = 4;
   private static final int DEPARTURE_TIME = 14000;

   private long chooseDepartureTime(long dayTime, int days) {
      long startOfDay = dayTime - dayTime % 24000;
      return startOfDay + days * 24000L + DEPARTURE_TIME;
   }

   private int randomStayDurationDays(RandomSource random) {
      return MIN_STAY_DAYS + random.nextInt(MAX_STAY_DAYS - MIN_STAY_DAYS + 1);
   }

   public boolean tryCheckIn(ServerLevel level, CivilizedVillager visitor) {
      VillagerInfo info = visitor.getInfo();
      ImpressionRequirement tier = info.getNpcRole().recruitTier();
      if (tier != ImpressionRequirement.MID && tier != ImpressionRequirement.HIGH)
         return false; // low tier visitors cannot stay in the in

      info.setHomeBuildingId(getBuilding().getBuildingId());
      visitor.getBrain()
            .setMemory(MemoryModuleType.HOME, new GlobalPos(level.dimension(), getBuilding().getBlockPos()));
      int stayDurationDays = randomStayDurationDays(visitor.getRandom());
      VisitorBehaviour.tryGet(visitor)
            .ifPresent(behaviour -> behaviour.setDepartAt(chooseDepartureTime(level.getDayTime(), stayDurationDays)));
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(info, StoreOperation.UPDATE);

      CurrencyItem.credit(getEntity().chests(), CURRENCY_RENT_PER_NIGHT * stayDurationDays);

      return true;
   }

   public static Optional<InnBehaviour> findAvailableInn(UUID settlementId) {
      return ServerBuildingsStore.INSTANCE.findForSettlement(settlementId)
            .stream()
            .map(LoadedBuildings::checkLoaded)
            .flatMap(Optional::stream)
            .filter(inn -> inn.getBehaviour() instanceof InnBehaviour innBehaviour && !innBehaviour.isFull())
            .map(b -> (InnBehaviour) b.getBehaviour())
            .findFirst();
   }

   public int getMaxVisitors() {
      int upgradeLevel = getBuilding().getUpgradeLevel();
      if (upgradeLevel <= 1)
         return 4;
      if (upgradeLevel == 2)
         return 6;
      return 8;
   }

   public boolean isFull() {
      return BuildingUtil.getVisitors(getBuilding(), ServerVillagerStore.INSTANCE).size() >= getMaxVisitors();
   }
}
