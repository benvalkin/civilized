package com.uncreated.civilized.entity.equipment;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CarriedSlotContainer implements Container {

   private final VillagerEquipment equipment;
   private final CarriedEquipmentSlot slot;

   public CarriedSlotContainer(VillagerEquipment equipment, CarriedEquipmentSlot slot) {
      this.equipment = equipment;
      this.slot = slot;
   }

   @Override
   public int getContainerSize() {
      return 1;
   }

   @Override
   public boolean isEmpty() {
      return !equipment.hasEquippedOrCarrying(slot);
   }

   @Override
   public @NotNull ItemStack getItem(int slotIndex) {
      return equipment.getEquippedOrCarried(slot);
   }

   @Override
   public @NotNull ItemStack removeItem(int slotIndex, int amount) {
      return equipment.remove(slot, amount);
   }

   @Override
   public @NotNull ItemStack removeItemNoUpdate(int slotIndex) {
      return equipment.remove(slot);
   }

   @Override
   public void setItem(int i, @NotNull ItemStack itemStack) {
      if (itemStack.isEmpty())
         equipment.remove(slot);
      else
         equipment.carry(itemStack, slot);
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
      equipment.clear(slot);
   }

   @Override
   public boolean canPlaceItem(int slotIndex, @NotNull ItemStack stack) {
      EquipmentSlot equipmentSlot = equipment.getEquipmentSlot(slot);
      if (equipmentSlot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR)
         return true; // if this slot isn't a wearable slot, all items can be placed in here

      return equipment.getVillager().getEquipmentSlotForItem(stack) == equipmentSlot;
   }
}
