package com.uncreated.civilized.core.building.state.animalfarm;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.logistics.hauling.VillagerInventoryType;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.InventoryStockRequirement;
import com.uncreated.civilized.core.building.state.BuildingState;
import com.uncreated.civilized.core.building.state.IItemManagementMenuSupplier;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.ui.menu.building.item.management.ItemManagementMenu;
import com.uncreated.civilized.ui.menu.building.worksite.animalfarm.items.ChooseAnimalFoodMenu;

import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public abstract class AnimalFarmState extends BuildingState implements IItemManagementMenuSupplier {

   public static final String FIELD_FOOD_SLOT = "food_slot_";

   private final ItemStack[] foodSlots;

   public static final int NUMBER_OF_FOOD_SLOTS = 3;

   @Getter
   private final InventoryStockRequirement animalFoodRequirement;

   public AnimalFarmState(Building building) {
      super(building);
      foodSlots = new ItemStack[NUMBER_OF_FOOD_SLOTS];
      foodSlots[0] = ItemStack.EMPTY;
      foodSlots[1] = ItemStack.EMPTY;
      foodSlots[2] = ItemStack.EMPTY;

      animalFoodRequirement =
            new InventoryStockRequirement(
                  building.getBuildingType().toString().toLowerCase() + "_animal_food",
                  this::isCorrectAnimalFood,
                  2,
                  8,
                  VillagerInventoryType.WORK_TASK);
   }

   public void applyNbt(CompoundTag compoundTag, HolderLookup.Provider registryAccess) {
      readItemSlot(0, compoundTag);
      readItemSlot(1, compoundTag);
      readItemSlot(2, compoundTag);
   }

   public CompoundTag toNbt(HolderLookup.Provider registryAccess) {
      CompoundTag tag = new CompoundTag();
      writeItemSlot(0, tag);
      writeItemSlot(1, tag);
      writeItemSlot(2, tag);
      return tag;
   }

   private void readItemSlot(int i, CompoundTag compoundTag) {
      String tagKey = FIELD_FOOD_SLOT + i;
      if (compoundTag.contains(tagKey))
         foodSlots[i] = ItemStack.parseOptional(building.getRegistryAccess(), compoundTag.getCompound(tagKey));
      else
         foodSlots[i] = ItemStack.EMPTY;
   }

   private void writeItemSlot(int i, CompoundTag tag) {
      String tagKey = FIELD_FOOD_SLOT + i;
      ItemStack itemStack = foodSlots[i];
      if (itemStack.isEmpty())
         return;

      tag.put(tagKey, foodSlots[i].save(building.getRegistryAccess()));
   }

   public final void tryApplyDefaults() {
      tryApplyDefaults(foodSlots);
   }

   public abstract void tryApplyDefaults(ItemStack[] foodSlots);

   public ItemStack getFoodSlot(int i) {
      return foodSlots[i];
   }

   public void setFoodSlot(int i, ItemStack itemStack) {
      foodSlots[i] = itemStack;
   }

   @Override
   public ItemManagementMenu createItemManagementMenu(
         Integer containerId,
         Inventory playerInventory,
         Building building,
         Settlement settlement) {
      return new ChooseAnimalFoodMenu(containerId, playerInventory, new SimpleContainer(3), settlement, building);
   }

   public abstract boolean isCorrectAnimalFood(ItemStack itemStack);
}
