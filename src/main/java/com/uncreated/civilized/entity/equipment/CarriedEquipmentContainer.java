package com.uncreated.civilized.entity.equipment;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A vanilla container view into a villager's current equipment slots. Intended for the player to add and remove
 * villager's equipment directly from their inventory. Only exists on the server and is synced to a simple vanilla
 * container on the client.
 * <p>
 * Unlike {@link CarriedSlotContainer} (used for hauling), this container force sets items into a villager's equipment
 * slots, without moving existing equipment into the overflow inventory, under the assumption that the player will have
 * taken out existing items into their inventory.
 */
public class CarriedEquipmentContainer implements Container {

   private final VillagerEquipment equipment;
   private final List<CarriedEquipmentSlot> slots;

   public CarriedEquipmentContainer(VillagerEquipment equipment, List<CarriedEquipmentSlot> slots) {
      this.equipment = equipment;
      this.slots = List.copyOf(slots);
   }

   @Override
   public int getContainerSize() {
      return slots.size();
   }

   @Override
   public boolean isEmpty() {
      for (CarriedEquipmentSlot slot : slots) {
         if (equipment.hasEquippedOrCarrying(slot))
            return false;
      }

      return true;
   }

   @Override
   public @NotNull ItemStack getItem(int slotIndex) {
      return equipment.getEquippedOrCarried(slots.get(slotIndex));
   }

   @Override
   public @NotNull ItemStack removeItem(int slotIndex, int amount) {
      return equipment.remove(slots.get(slotIndex), amount);
   }

   @Override
   public @NotNull ItemStack removeItemNoUpdate(int slotIndex) {
      return equipment.remove(slots.get(slotIndex));
   }

   @Override
   public void setItem(int slotIndex, @NotNull ItemStack itemStack) {
      // it is expected that there isn't anything in this equipment slot
      equipment.forceSet(slots.get(slotIndex), itemStack);
   }

   @Override
   public void setChanged() {
   }

   @Override
   public boolean stillValid(Player player) {
      return player.position().distanceToSqr(equipment.getVillager().position()) < 8 * 8;
   }

   @Override
   public void clearContent() {
      for (CarriedEquipmentSlot slot : slots)
         equipment.clear(slot);
   }

   @Override
   public boolean canPlaceItem(int slotIndex, @NotNull ItemStack stack) {
      return equipment.canInsertInto(slots.get(slotIndex), stack);
   }
}
