package com.uncreated.civilized.core.building.entity;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

@Getter
public class LoadedBuilding {
   private final Building building;
   private final Level level;
   private final BuildingBehaviour behaviour;
   private final Map<CivilizedVillager, ItemReservation> itemReservations;
   private final Set<ChestBlockEntity> chests = new LinkedHashSet<>();

   public LoadedBuilding(Building building, Level level) {
      this.building = building;
      this.level = level;
      behaviour = building.getBuildingType().createBehaviour().apply(this);
      behaviour.start();
      itemReservations = new HashMap<>();

      findChestsInsideBounds();
   }

   /**
    * Only intended to be called once when this building is loaded.
    */
   private void findChestsInsideBounds() {
      building.getBounds()
            .getBlockEntitiesInsideBuilding(level)
            .stream()
            .filter(e -> e instanceof ChestBlockEntity)
            .forEach(e -> chests.add((ChestBlockEntity) e));
   }

   public void onChestLoaded(ChestBlockEntity chest) {
      chests.add(chest);
   }

   public void onChestUnloaded(ChestBlockEntity chest) {
      chests.remove(chest);
   }

   public void reserveItems(CivilizedVillager villager, String key, Predicate<ItemStack> matching, int amount) {
      itemReservations.put(villager, new ItemReservation(key, matching, amount));
   }

   public void cancelReservations(CivilizedVillager villager) {
      itemReservations.remove(villager);
   }

   public List<Container> chests() {
      chests.removeIf(BlockEntity::isRemoved);
      return chests.stream().map(c -> ((Container) c)).toList();
   }

   public Optional<ChestBlockEntity> anyChest() {
      chests.removeIf(BlockEntity::isRemoved);
      return chests.stream().findFirst();
   }

   @Override
   public String toString() {
      return building.toString();
   }
}
