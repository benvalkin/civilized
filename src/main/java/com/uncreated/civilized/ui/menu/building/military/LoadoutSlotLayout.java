package com.uncreated.civilized.ui.menu.building.military;

import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * How a loadout's slots are laid out and shown, so that the screens showing a soldier's equipment and editing a
 * loadout look alike: the weapons on the left, and the armour on the right, head and chest above legs and feet.
 */
public final class LoadoutSlotLayout {

   public record GridCell(int column, int row) {
   }

   private LoadoutSlotLayout() {
   }

   public static GridCell gridCellFor(CarriedEquipmentSlot slot) {
      return switch (slot) {
      case PRIMARY_WEAPON -> new GridCell(0, 0);
      case SECONDARY_WEAPON -> new GridCell(1, 0);
      case OFFHAND_WEAPON -> new GridCell(0, 1);
      case HEAD -> new GridCell(2, 0);
      case CHEST -> new GridCell(3, 0);
      case LEGS -> new GridCell(2, 1);
      case FEET -> new GridCell(3, 1);
      case TOOL -> throw new IllegalArgumentException("Loadouts don't have a tool slot");
      };
   }

   public static ResourceLocation watermarkIconFor(CarriedEquipmentSlot slot) {
      String sprite = switch (slot) {
      case PRIMARY_WEAPON -> "sword";
      case SECONDARY_WEAPON -> "axe";
      case OFFHAND_WEAPON -> "shield";
      case HEAD -> "helmet";
      case CHEST -> "chestplate";
      case LEGS -> "leggings";
      case FEET -> "boots";
      case TOOL -> "pickaxe";
      };
      return ResourceLocation.withDefaultNamespace("container/slot/" + sprite);
   }

   public static Component nameOf(CarriedEquipmentSlot slot) {
      return Component.translatable("menu.building.military.loadout.edit.slot." + slot.name().toLowerCase());
   }
}
