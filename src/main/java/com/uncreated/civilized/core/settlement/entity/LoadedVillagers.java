package com.uncreated.civilized.core.settlement.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.uncreated.civilized.entity.CivilizedVillager;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

public class LoadedVillagers {

   private static final Logger LOGGER = LogUtils.getLogger();

   private final static Map<UUID, CivilizedVillager> loadedVillagers = new HashMap<>();

   public static void onVillagerJoinLevel(CivilizedVillager villager) {
      loadedVillagers.put(villager.getVillagerId(), villager);
   }

   public static void onVillagerLeaveLevel(CivilizedVillager villager) {
      loadedVillagers.remove(villager.getVillagerId());
   }

   public static Optional<CivilizedVillager> find(UUID villagerId) {
      return Optional.ofNullable(loadedVillagers.get(villagerId));
   }
   public static CivilizedVillager get(UUID villagerId) {
      return find(villagerId).orElseThrow();
   }

   public static ImmutableList<CivilizedVillager> all() {
      return ImmutableList.copyOf(loadedVillagers.values());
   }

   public static ImmutableList<CivilizedVillager> matching(Iterable<UUID> villagerIds) {
      List<CivilizedVillager> resultSet = new ArrayList<>();
      for (UUID villagerId : villagerIds) {
         CivilizedVillager civilizedVillager = loadedVillagers.get(villagerId);
         if (civilizedVillager == null)
            continue;

         resultSet.add(civilizedVillager);
      }

      return ImmutableList.copyOf(resultSet);
   }
}
