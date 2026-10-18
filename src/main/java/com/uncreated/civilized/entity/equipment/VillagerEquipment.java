package com.uncreated.civilized.entity.equipment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.entity.CivilizedVillager;

import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class VillagerEquipment {

   @Getter
   private final CivilizedVillager villager;
   private ItemStack[] carriedSlots;
   private Map<CarriedEquipmentSlot, EquipmentSlot> equipment;
   private final Map<EquipmentSlot, CarriedEquipmentSlot> currentlyEquipped = new HashMap<>();

   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String FIELD_CARRIED = "carried_equipment";
   private static final String FIELD_EQUIPPED_FROM = "equipped_from";

   public static final List<CarriedEquipmentSlot> COMBAT_SLOTS =
           List.of(
                   CarriedEquipmentSlot.PRIMARY_WEAPON,
                   CarriedEquipmentSlot.SECONDARY_WEAPON,
                   CarriedEquipmentSlot.OFFHAND_WEAPON,
                   CarriedEquipmentSlot.HEAD,
                   CarriedEquipmentSlot.CHEST,
                   CarriedEquipmentSlot.LEGS,
                   CarriedEquipmentSlot.FEET);

   public VillagerEquipment(CivilizedVillager villager) {
      this.villager = villager;
      this.carriedSlots = emptyCarriedSlots();
      this.equipment = new HashMap<>();

      equipment.put(CarriedEquipmentSlot.PRIMARY_WEAPON, EquipmentSlot.MAINHAND);
      equipment.put(CarriedEquipmentSlot.SECONDARY_WEAPON, EquipmentSlot.MAINHAND);
      equipment.put(CarriedEquipmentSlot.OFFHAND_WEAPON, EquipmentSlot.OFFHAND);
      equipment.put(CarriedEquipmentSlot.TOOL, EquipmentSlot.MAINHAND);
      equipment.put(CarriedEquipmentSlot.HEAD, EquipmentSlot.HEAD);
      equipment.put(CarriedEquipmentSlot.CHEST, EquipmentSlot.CHEST);
      equipment.put(CarriedEquipmentSlot.LEGS, EquipmentSlot.LEGS);
      equipment.put(CarriedEquipmentSlot.FEET, EquipmentSlot.FEET);
   }

   private static ItemStack @NotNull [] emptyCarriedSlots() {
      ItemStack[] itemStacks = new ItemStack[CarriedEquipmentSlot.values().length];
      Arrays.fill(itemStacks, ItemStack.EMPTY);
      return itemStacks;
   }

   public void save(CompoundTag tag, HolderLookup.Provider registryAccess) {

      CompoundTag carriedTag = new CompoundTag();
      for (CarriedEquipmentSlot slot : CarriedEquipmentSlot.values()) {
         ItemStack carried = carriedSlots[slot.ordinal()];
         if (!carried.isEmpty())
            carriedTag.put(slot.name(), carried.save(registryAccess));
      }
      tag.put(FIELD_CARRIED, carriedTag);

      CompoundTag equippedFromTag = new CompoundTag();
      currentlyEquipped
            .forEach((equipmentSlot, origin) -> equippedFromTag.putString(equipmentSlot.getName(), origin.name()));
      tag.put(FIELD_EQUIPPED_FROM, equippedFromTag);
   }

   public void load(CompoundTag tag, HolderLookup.Provider registryAccess) {
      carriedSlots = emptyCarriedSlots();
      currentlyEquipped.clear();

      CompoundTag carriedTag = tag.getCompound(FIELD_CARRIED);

      for (CarriedEquipmentSlot slot : CarriedEquipmentSlot.values()) {
         if (!carriedTag.contains(slot.name()))
            continue;

         CompoundTag compound = carriedTag.getCompound(slot.name());
         Optional<ItemStack> itemStack = ItemStack.parse(registryAccess, compound);
         itemStack.ifPresent(i -> carriedSlots[slot.ordinal()] = i);
      }

      CompoundTag equippedFromTag = tag.getCompound(FIELD_EQUIPPED_FROM);
      for (String equipmentSlotName : equippedFromTag.getAllKeys()) {
         Optional<EquipmentSlot> equipmentSlot = parseEquipmentSlot(equipmentSlotName);
         Optional<CarriedEquipmentSlot> origin = parseCarriedSlot(equippedFromTag.getString(equipmentSlotName));

         // sanity check that the origin still actually works with its supposed equipment slot
         if (equipmentSlot.isEmpty() || origin.isEmpty())
            continue;

         if (equipment.get(origin.get()) != equipmentSlot.get())
            continue;

         if (!hasEquipped(equipmentSlot.get()))
            continue;

         currentlyEquipped.put(equipmentSlot.get(), origin.get());
      }
   }

   private static Optional<CarriedEquipmentSlot> parseCarriedSlot(String name) {
      try {
         return Optional.of(CarriedEquipmentSlot.valueOf(name));
      } catch (IllegalArgumentException e) {
         return Optional.empty();
      }
   }

   private static Optional<EquipmentSlot> parseEquipmentSlot(String name) {
      try {
         return Optional.of(EquipmentSlot.byName(name));
      } catch (IllegalArgumentException e) {
         return Optional.empty();
      }
   }

   public void markEquipped(EquipmentSlot equipmentSlot, CarriedEquipmentSlot carriedOrigin) {
      currentlyEquipped.put(equipmentSlot, carriedOrigin);
   }

   public void markUnequipped(EquipmentSlot equipmentSlot) {
      currentlyEquipped.remove(equipmentSlot);
   }

   public Optional<CarriedEquipmentSlot> getOriginalCarriedSlot(EquipmentSlot equipmentSlot) {
      return Optional.ofNullable(currentlyEquipped.get(equipmentSlot));
   }

   public boolean canEquipFromCarried(CarriedEquipmentSlot carriedEquipmentSlot) {
      ItemStack carried = carriedSlots[carriedEquipmentSlot.ordinal()];
      if (carried.isEmpty())
         return false;

      EquipmentSlot equipmentSlot = equipment.get(carriedEquipmentSlot);
      ItemStack alreadyEquipped = villager.getItemBySlot(equipmentSlot);
      if (!alreadyEquipped.isEmpty()) {
         Optional<CarriedEquipmentSlot> carriedSlotOfAlreadyEquipped = getOriginalCarriedSlot(equipmentSlot);
         if (carriedSlotOfAlreadyEquipped.isEmpty())
            return false; // not sure where the already equipped item came from

         return carriedSlots[carriedSlotOfAlreadyEquipped.get().ordinal()].isEmpty();
      }

      return true;
   }

   public boolean unequip(EquipmentSlot equipmentSlot) {
      ItemStack alreadyEquipped = villager.getItemBySlot(equipmentSlot);
      if (alreadyEquipped.isEmpty()) {
         // nothing to unequip
         markUnequipped(equipmentSlot); // just in case
         return true;
      }

      Optional<CarriedEquipmentSlot> carriedSlotOfAlreadyEquipped = getOriginalCarriedSlot(equipmentSlot);
      if (carriedSlotOfAlreadyEquipped.isEmpty()) {
         // not sure where to put the already equipped item - try add it to the overflow inventory
         if (!tryMoveToOverflowInventory(equipmentSlot))
            return false;
      } else {
         ItemStack carried = carriedSlots[carriedSlotOfAlreadyEquipped.get().ordinal()];
         if (!carried.isEmpty()) {
            // the carried slot of the equipped item for some reason has another item in it
            if (!tryMoveToOverflowInventory(carriedSlotOfAlreadyEquipped.get()))
               return false;
         }
         // corresponding carried slot is empty - we can move the already equipped item here
         carriedSlots[carriedSlotOfAlreadyEquipped.get().ordinal()] = alreadyEquipped.copy();
      }

      villager.setItemSlot(equipmentSlot, ItemStack.EMPTY);
      markUnequipped(equipmentSlot);
      return true;
   }

   public boolean unequip(CarriedEquipmentSlot carriedEquipmentSlot) {
      if (hasEquipped(carriedEquipmentSlot))
         return unequip(equipment.get(carriedEquipmentSlot));

      return true;
   }

   public void unequip(List<CarriedEquipmentSlot> equipmentSlots) {
      for (CarriedEquipmentSlot slot : equipmentSlots)
         villager.getEquipment().unequip(slot);
   }

   public EquipmentSlot getEquipmentSlot(CarriedEquipmentSlot carriedEquipmentSlot) {
      return equipment.get(carriedEquipmentSlot);
   }

   public boolean hasEquipped(EquipmentSlot equipmentSlot) {
      return !villager.getItemBySlot(equipmentSlot).isEmpty();
   }

   public boolean hasEquipped(CarriedEquipmentSlot carriedEquipmentSlot) {
      EquipmentSlot equipmentSlot = equipment.get(carriedEquipmentSlot);
      return currentlyEquipped.get(equipmentSlot) == carriedEquipmentSlot
            && !villager.getItemBySlot(equipmentSlot).isEmpty();
   }

   public boolean isCarrying(CarriedEquipmentSlot carriedEquipmentSlot) {
      return !carriedSlots[carriedEquipmentSlot.ordinal()].isEmpty();
   }

   public boolean hasEquippedOrCarrying(CarriedEquipmentSlot carriedEquipmentSlot) {
      return hasEquipped(carriedEquipmentSlot) || isCarrying(carriedEquipmentSlot);
   }

   public ItemStack getEquippedOrCarried(CarriedEquipmentSlot carriedEquipmentSlot) {
      EquipmentSlot equipmentSlot = equipment.get(carriedEquipmentSlot);
      boolean thisSlotIsEquipped = currentlyEquipped.get(equipmentSlot) == carriedEquipmentSlot;
      if (thisSlotIsEquipped)
         return villager.getItemBySlot(equipmentSlot);

      return carriedSlots[carriedEquipmentSlot.ordinal()];
   }

   public boolean carry(ItemStack newEquipment, CarriedEquipmentSlot carriedEquipmentSlot) {
      if (newEquipment.isEmpty())
         return false;

      if (hasEquipped(carriedEquipmentSlot)) {
         if (!unequip(equipment.get(carriedEquipmentSlot)))
            return false;
      }

      if (isCarrying(carriedEquipmentSlot)) {
         if (!tryMoveToOverflowInventory(carriedEquipmentSlot))
            return false;
      }

      carriedSlots[carriedEquipmentSlot.ordinal()] = newEquipment;
      return true;
   }

   public ItemStack remove(CarriedEquipmentSlot carriedEquipmentSlot, int amount) {

      if (hasEquipped(carriedEquipmentSlot)) {
         EquipmentSlot equipmentSlot = equipment.get(carriedEquipmentSlot);
         ItemStack equipped = villager.getItemBySlot(equipmentSlot);
         ItemStack removed = equipped.copyWithCount(Math.min(amount, equipped.getCount()));
         equipped.shrink(amount);

         if (equipped.isEmpty())
            markUnequipped(equipmentSlot);

         return removed;
      } else {
         ItemStack carried = carriedSlots[carriedEquipmentSlot.ordinal()];
         ItemStack removed = carried.copyWithCount(Math.min(amount, carried.getCount()));
         carried.shrink(amount);
         return removed;
      }
   }

   public ItemStack remove(CarriedEquipmentSlot carriedEquipmentSlot) {
      return remove(carriedEquipmentSlot, Integer.MAX_VALUE);
   }

   public void clear(CarriedEquipmentSlot carriedEquipmentSlot) {
      EquipmentSlot equipmentSlot = equipment.get(carriedEquipmentSlot);
      villager.setItemSlot(equipmentSlot, ItemStack.EMPTY);
      markUnequipped(equipmentSlot);
      carriedSlots[carriedEquipmentSlot.ordinal()] = ItemStack.EMPTY;
   }

   public void clearAllSlots() {
      carriedSlots = emptyCarriedSlots();
      currentlyEquipped.clear();
   }

   public boolean equip(CarriedEquipmentSlot carriedEquipmentSlot) {
      ItemStack carried = carriedSlots[carriedEquipmentSlot.ordinal()].copy();
      if (carried.isEmpty())
         return true;

      EquipmentSlot equipmentSlot = equipment.get(carriedEquipmentSlot);
      ItemStack alreadyEquipped = villager.getItemBySlot(equipmentSlot).copy();
      if (!alreadyEquipped.isEmpty()) {
         // need to put away what's already equipped
         Optional<CarriedEquipmentSlot> carriedSlotOfAlreadyEquipped = getOriginalCarriedSlot(equipmentSlot);
         if (carriedSlotOfAlreadyEquipped.isPresent()) {
            if (!carriedSlots[carriedSlotOfAlreadyEquipped.get().ordinal()].isEmpty()) {
               if (!tryMoveToOverflowInventory(carriedSlotOfAlreadyEquipped.get()))
                  return false;
            }

            // take out carried
            // (order kinda matters here to avoid accidentally overwriting carried items)
            carriedSlots[carriedEquipmentSlot.ordinal()] = ItemStack.EMPTY;
            // put back existing
            carriedSlots[carriedSlotOfAlreadyEquipped.get().ordinal()] = alreadyEquipped;
            // equip
            villager.setItemSlot(equipmentSlot, carried);
            markEquipped(equipmentSlot, carriedEquipmentSlot);
            return true;
         } else
            // not sure where to put the already equipped item back - rather don't try to equip another one
            return false;
      } else {
         villager.setItemSlot(equipmentSlot, carried);
         carriedSlots[carriedEquipmentSlot.ordinal()] = ItemStack.EMPTY;
         markEquipped(equipmentSlot, carriedEquipmentSlot);
         return true;
      }
   }

   public void equip(List<CarriedEquipmentSlot> equipmentSlots) {
      for (CarriedEquipmentSlot slot : equipmentSlots)
         villager.getEquipment().equip(slot);
   }

   public boolean moveOutCurrentEquipment(CarriedEquipmentSlot carriedEquipmentSlot) {
      if (hasEquipped(carriedEquipmentSlot)) {
         EquipmentSlot equipmentSlot = equipment.get(carriedEquipmentSlot);
         return tryMoveToOverflowInventory(equipmentSlot);
      } else {
         return tryMoveToOverflowInventory(carriedEquipmentSlot);
      }
   }

   private boolean tryMoveToOverflowInventory(CarriedEquipmentSlot carriedEquipmentSlot) {
      SimpleContainer overflowInventory = getOverflowInventory();
      ItemStack carried = carriedSlots[carriedEquipmentSlot.ordinal()];

      ItemStack remainder = overflowInventory.addItem(carried);
      carriedSlots[carriedEquipmentSlot.ordinal()] = remainder;
      return remainder.isEmpty();
   }

   private boolean tryMoveToOverflowInventory(EquipmentSlot equipmentSlot) {
      SimpleContainer overflowInventory = getOverflowInventory();
      ItemStack equipped = villager.getItemBySlot(equipmentSlot);

      ItemStack remainder = overflowInventory.addItem(equipped);
      villager.setItemSlot(equipmentSlot, remainder);
      return remainder.isEmpty();
   }

   @NotNull
   public SimpleContainer getOverflowInventory() {
      // work inventory is the overflow inventory for equipment because it benefits most workers to still have access to
      // the item here
      return villager.getWorkInputInventory();
   }
}
