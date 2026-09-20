package com.uncreated.civilized.core.building.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.entity.behaviour.BuildingBehaviour;
import com.uncreated.civilized.core.building.logistics.hauling.ItemReservation;
import com.uncreated.civilized.entity.CivilizedVillager;

import lombok.Getter;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

@Getter
public class LoadedBuilding {
   private final Building building;
   private final Level level;
   private final BuildingBehaviour behaviour;
   private final Map<CivilizedVillager, ItemReservation> itemReservations;

   public LoadedBuilding(Building building, Level level) {
      this.building = building;
      this.level = level;
      behaviour = building.getBuildingType().createBehaviour().apply(this);
      behaviour.start();
      itemReservations = new HashMap<>();
   }

   public void reserveItems(CivilizedVillager villager, String key, Predicate<ItemStack> matching, int amount) {
      itemReservations.put(villager, new ItemReservation(key, matching, amount));
   }

   public void cancelReservations(CivilizedVillager villager) {
      itemReservations.remove(villager);
   }

   public List<Container> findChests() {
      return building.getBounds()
            .getBlockEntitiesInsideBuilding(getLevel())
            .stream()
            .filter(e -> e instanceof ChestBlockEntity)
            .map(e -> ((Container) e))
            .toList();
   }

   public Optional<ChestBlockEntity> findAnyChest() {
      return building.getBounds()
            .getBlockEntitiesInsideBuilding(getLevel())
            .stream()
            .filter(e -> e instanceof ChestBlockEntity)
            .map(e -> ((ChestBlockEntity) e))
            .findFirst();
   }

   @Override
   public String toString() {
      return building.toString();
   }
}
