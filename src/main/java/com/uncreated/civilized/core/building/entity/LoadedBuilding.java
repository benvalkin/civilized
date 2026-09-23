package com.uncreated.civilized.core.building.entity;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.entity.behaviour.BuildingBehaviour;
import com.uncreated.civilized.core.building.logistics.hauling.ItemReservation;

import lombok.AccessLevel;
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
   @Getter(AccessLevel.PRIVATE)
   private final Map<String, Map<String, ItemReservation>> partyItemReservations;
   private final Set<ChestBlockEntity> chests = new LinkedHashSet<>();

   public LoadedBuilding(Building building, Level level) {
      this.building = building;
      this.level = level;
      behaviour = building.getBuildingType().createBehaviour().apply(this);
      behaviour.start();
      partyItemReservations = new LinkedHashMap<>();

      findChestsInsideBounds();
   }

   public void placeReservation(String party, String reservationName, Predicate<ItemStack> matching, int amount) {
      partyItemReservations.compute(party, (k, reservations) -> {
         if (reservations == null) {
            Map<String, ItemReservation> reservationsNew = new LinkedHashMap<>();
            reservationsNew.put(reservationName, new ItemReservation(party, reservationName, matching, amount));
            return reservationsNew;
         } else {
            reservations.put(reservationName, new ItemReservation(party, reservationName, matching, amount));
            return reservations;
         }
      });
   }

   public List<ItemReservation> getReservationsExcluding(String party) {
      List<ItemReservation> others = new LinkedList<>();
      for (Map.Entry<String, Map<String, ItemReservation>> entry : partyItemReservations.entrySet()) {
         if (entry.getKey().equals(party))
            continue;

         others.addAll(entry.getValue().values());
      }
      return others;
   }

   public Optional<ItemReservation> cancelReservation(String party, String reservationName) {
      Map<String, ItemReservation> reservations = partyItemReservations.get(party);
      if (reservations == null)
         return Optional.empty();

      ItemReservation removed = reservations.remove(reservationName);

      if (reservations.isEmpty())
         partyItemReservations.remove(party);

      return Optional.ofNullable(removed);
   }

   public void cancelReservationForParty(String party) {
      partyItemReservations.remove(party);
   }

   public void cancelAllReservations() {
      partyItemReservations.clear();
   }

   public List<Container> chests() {
      // NOTE: the loaded events do not account for if a chest destroyed by an explosion, pushed by a piston, placed by
      // a dispenser or another mod etc.
      // So we should clean out stale entities
      chests.removeIf(BlockEntity::isRemoved);
      return chests.stream().map(c -> ((Container) c)).toList();
   }

   public Optional<ChestBlockEntity> anyChest() {
      chests.removeIf(BlockEntity::isRemoved);
      return chests.stream().findFirst();
   }

   // Only intended to be called once when this building is loaded.
   private void findChestsInsideBounds() {
      building.getBounds()
            // do not load extra chunks when looking for chest entities!
            // chest entities in other chunks that are part of this building will be added to this building later when
            // the chunk loads.
            .getBlockEntitiesInsideBuilding(level, false)
            .stream()
            .filter(e -> e instanceof ChestBlockEntity)
            .forEach(e -> chests.add((ChestBlockEntity) e));
   }

   public void onChestLoaded(ChestBlockEntity chest) {
      // NOTE: this method can still run after findChestsInsideBounds() - we use a Set to avoid double counting
      chests.add(chest);
   }

   public void onChestUnloaded(ChestBlockEntity chest) {
      chests.remove(chest);
   }

   @Override
   public String toString() {
      return building.toString();
   }

   @Override
   public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass())
         return false;
      LoadedBuilding building1 = (LoadedBuilding) o;
      return Objects.equals(building.getBuildingId(), building1.building.getBuildingId());
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(building.getBuildingId());
   }
}
