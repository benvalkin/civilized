package com.uncreated.civilized.ui.menu.villager;

import java.util.function.BooleanSupplier;

import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;
import com.uncreated.civilized.ui.menu.building.military.LoadoutSlotLayout;
import com.uncreated.civilized.ui.menu.container.TogglableSlot;

import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class EquipmentMenuSlot extends TogglableSlot {

   private final CivilizedVillager villager;
   @Getter
   private final CarriedEquipmentSlot equipmentSlot;
   private final BooleanSupplier canEdit;

   public EquipmentMenuSlot(
         Container container,
         int slotIndex,
         int x,
         int y,
         BooleanSupplier isVisible,
         CivilizedVillager villager,
         CarriedEquipmentSlot equipmentSlot,
         BooleanSupplier canEdit) {
      super(container, slotIndex, x, y, isVisible);
      this.villager = villager;
      this.equipmentSlot = equipmentSlot;
      this.canEdit = canEdit;
   }

   @Override
   public boolean mayPlace(ItemStack stack) {
      // note: permission and matching item checks are performed in this slot class and not in the parent container
      // where they would typically be done.
      // This is because the client and server both use EquipmentMenuSlot in the GUI, but they do not use the same
      // container classes to represent the villager's equipment. The server uses a specialized container, whereas the
      // client uses a simple vanilla one that cannot perform custom placement checks.
      // Therefore, we do these checks in the slot, because this method runs on both the client and server. The
      // typical approach would lead to a brief desync flicker when dragging disallowed items into an equipment slot.
      return canEdit.getAsBoolean() && villager.getEquipment().canInsertInto(equipmentSlot, stack);
   }

   @Override
   public boolean mayPickup(Player player) {
      return canEdit.getAsBoolean();
   }

   @Override
   public int getMaxStackSize() {
      return 1;
   }

   @Override
   public ResourceLocation getNoItemIcon() {
      return LoadoutSlotLayout.watermarkIconFor(equipmentSlot);
   }
}
