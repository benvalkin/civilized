package com.uncreated.civilized.core.building.state;

import java.util.Arrays;

import com.uncreated.civilized.core.building.Building;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;

public class CropFarmState extends BuildingState implements IEyeDropperSlotState {

   public static final String FIELD_CROP_SLOT = "crop_slot_";

   private final ItemStack[] cropSlots;

   public static final int NUMBER_OF_CROP_SLOTS = 3;

   public CropFarmState(Building building) {
      super(building);
      cropSlots = new ItemStack[NUMBER_OF_CROP_SLOTS];
      cropSlots[0] = ItemStack.EMPTY;
      cropSlots[1] = ItemStack.EMPTY;
      cropSlots[2] = ItemStack.EMPTY;
      tryApplyDefaults();
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
      String tagKey = FIELD_CROP_SLOT + i;
      if (compoundTag.contains(tagKey))
         cropSlots[i] = ItemStack.parseOptional(building.getRegistryAccess(), compoundTag.getCompound(tagKey));
      else
         cropSlots[i] = ItemStack.EMPTY;
   }

   private void writeItemSlot(int i, CompoundTag tag) {
      String tagKey = FIELD_CROP_SLOT + i;
      ItemStack itemStack = cropSlots[i];
      if (itemStack.isEmpty())
         return;

      tag.put(tagKey, cropSlots[i].save(building.getRegistryAccess()));
   }

   /**
    * Fills in the field's usual crops, but only when nothing has been chosen at all, so the player's choices are never
    * overwritten. Done on both sides whenever the state is created or read, so that the server plants the same crops
    * the player sees on screen.
    */
   public void tryApplyDefaults() {
      if (Arrays.stream(cropSlots).allMatch(ItemStack::isEmpty)) {
         cropSlots[0] = new ItemStack(Items.WHEAT_SEEDS);
         cropSlots[1] = new ItemStack(Items.CARROT);
         cropSlots[2] = new ItemStack(Items.POTATO);
      }
   }

   public boolean isCorrectCrop(ItemStack stack) {
      return Arrays.stream(cropSlots).anyMatch(i -> ItemStack.isSameItem(i, stack));
   }

   public ItemStack getCropSlot(int i) {
      return cropSlots[i];
   }

   public void setCropSlot(int i, ItemStack itemStack) {
      cropSlots[i] = itemStack;
   }

   /** Whether the item plants a crop, i.e. whether it can be chosen for a crop slot. */
   public static boolean isPlantableCrop(ItemStack stack) {
      return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof CropBlock;
   }

   @Override
   public int getEyeDropperSlotCount() {
      return NUMBER_OF_CROP_SLOTS;
   }

   @Override
   public ItemStack getEyeDropperSlotItem(int slot) {
      return getCropSlot(slot);
   }

   @Override
   public boolean isValidEyeDropperSlotItem(int slot, ItemStack stack) {
      return stack.isEmpty() || isPlantableCrop(stack);
   }

   @Override
   public void setEyeDropperSlotItem(int slot, ItemStack stack) {
      setCropSlot(slot, stack);

      // clearing the last crop slot brings the defaults back straight away, the same as the client does when it reads
      // the change, so both sides keep planting the same crops
      tryApplyDefaults();
   }
}
