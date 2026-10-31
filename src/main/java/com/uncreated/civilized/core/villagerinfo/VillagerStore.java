package com.uncreated.civilized.core.villagerinfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingStore;

import net.minecraft.world.level.saveddata.SavedData;

public abstract class VillagerStore extends SavedData {

   protected static final Logger LOGGER = LogUtils.getLogger();

   protected VillagerInfoDB villagers;

   protected VillagerStore() {
      villagers = new VillagerInfoDB();
   }

   public ImmutableList<VillagerInfo> all() {
      return villagers.all();
   }

   public Optional<VillagerInfo> find(UUID villagerId) {
      return villagers.find(villagerId);
   }

   public Set<VillagerInfo> getCitizens(UUID settlementId) {
      return villagers.getSettlementsToVillagersIndex().getValues(settlementId);
   }

   public Set<VillagerInfo> getOccupants(UUID buildingId) {
      return villagers.getHomesToVillagersIndex().getValues(buildingId);
   }

   public List<VillagerInfo> getVisitors(Building building) {
      List<VillagerInfo> visitors = new ArrayList<>();
      for (VillagerInfo occupant : getOccupants(building.getBuildingId())) {
         if (!Objects.equals(occupant.getSettlementId(), building.getSettlementId()))
            visitors.add(occupant);
      }
      return visitors;
   }

   public List<VillagerInfo> getVisitors(UUID settlementId, BuildingStore buildings) {
      List<VillagerInfo> visitors = new ArrayList<>();
      if (settlementId == null)
         return visitors;

      for (Building building : buildings.findForSettlement(settlementId))
         visitors.addAll(getVisitors(building));
      return visitors;
   }

   public Set<VillagerInfo> findChildren(UUID parentId) {
      return villagers.getParentsToChildrenIndex().getValues(parentId);
   }

   public Set<VillagerInfo> findSiblings(VillagerInfo villager) {
      Set<VillagerInfo> siblings = new HashSet<>();
      for (UUID parentId : villager.getParentIds())
         siblings.addAll(findChildren(parentId));
      siblings.remove(villager);
      return siblings;
   }

   public VillagerInfo get(UUID villagerId) {
      return villagers.get(villagerId);
   }
}
