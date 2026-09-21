package com.uncreated.civilized.core.building.state.animalfarm;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.logistics.hauling.VillagerInventoryType;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.InventoryStockRequirement;
import com.uncreated.civilized.core.building.state.BuildingState;
import com.uncreated.civilized.core.building.state.IEyeDropperSlotState;

import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public abstract class AnimalFarmState extends BuildingState implements IEyeDropperSlotState {

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
      tryApplyDefaults();

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
      tryApplyDefaults();
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

   /**
    * Fills in the farm's usual food, but only when nothing has been chosen at all, so the player's choices are never
    * overwritten. Done on both sides whenever the state is created or read, so that the server breeds with the same
    * food the player sees on screen.
    */
   public final void tryApplyDefaults() {
      for (ItemStack foodSlot : foodSlots) {
         if (!foodSlot.isEmpty())
            return;
      }

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
   public int getEyeDropperSlotCount() {
      return NUMBER_OF_FOOD_SLOTS;
   }

   @Override
   public ItemStack getEyeDropperSlotItem(int slot) {
      return getFoodSlot(slot);
   }

   @Override
   public boolean isValidEyeDropperSlotItem(int slot, ItemStack stack) {
      return stack.isEmpty() || isCorrectAnimalFood(stack);
   }

   @Override
   public void setEyeDropperSlotItem(int slot, ItemStack stack) {
      setFoodSlot(slot, stack);

      // clearing the last food slot brings the defaults back straight away, the same as the client does when it reads
      // the change, so both sides keep breeding with the same food
      tryApplyDefaults();
   }

   public abstract boolean isCorrectAnimalFood(ItemStack itemStack);
}
