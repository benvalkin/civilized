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
      // these checks will only run on the server (since the client's slot is a regular slot).
      // todo: won't this it cause carried item desync if the check is not performed on the client? The client could
      // allow placing an item in the wrong slot (e.g. sword in the armor slot), but the server wouldn't allow it
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
