package com.uncreated.civilized.core.building.logistics.hauling.requirement;

import com.uncreated.civilized.core.building.production.bills.ItemFilter;
import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;
import com.uncreated.civilized.entity.equipment.CarriedSlotContainer;

import lombok.Getter;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class EquipmentRequirement extends InventoryStockRequirement {

   @Getter
   private final CarriedEquipmentSlot slot;

   private EquipmentRequirement(CarriedEquipmentSlot slot, ItemFilter allowed, Predicate<ItemStack> searchFunction) {
      super(
            "take_" + slot.name().toLowerCase(),
            searchFunction,
            allowed.leftMostPreferred(),
            1,
            1,
            // the slot only looks full once it holds something this requirement wants, so a worse item can be replaced
            villager -> new CarriedSlotContainer(villager.getEquipment(), slot));
      this.slot = slot;
   }

   public static EquipmentRequirement allowedBy(CarriedEquipmentSlot slot, ItemFilter allowed) {
      return new EquipmentRequirement(slot, allowed, allowed::acceptsItem);
   }

   public static EquipmentRequirement onlyBestPossibleEquipment(CarriedEquipmentSlot slot, ItemFilter allowed, ItemStack bestEquipmentAvailable) {
      int bestEquipmentAvailableScore = allowed.preferenceIndex(bestEquipmentAvailable);
      return new EquipmentRequirement(slot, allowed, i -> allowed.acceptsItem(i) && allowed.preferenceIndex(i) <= bestEquipmentAvailableScore);
   }
}
