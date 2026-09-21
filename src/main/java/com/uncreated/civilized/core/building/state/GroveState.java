package com.uncreated.civilized.core.building.state;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.logistics.hauling.VillagerInventoryType;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.InventoryStockRequirement;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SaplingBlock;

public class GroveState extends BuildingState implements IEyeDropperSlotState {

   public static final String FIELD_SAPLING = "sapling";

   private static final ItemStack DEFAULT_SAPLING = new ItemStack(Items.OAK_SAPLING);

   @Getter
   @Setter
   private ItemStack sapling;

   @Getter
   private final InventoryStockRequirement saplingRequirement;

   public GroveState(Building building) {
      super(building);
      sapling = DEFAULT_SAPLING;

      saplingRequirement =
            new InventoryStockRequirement(
                  building.getBuildingType().toString().toLowerCase() + "_saplings",
                  this::isCorrectSapling,
                  1,
                  16,
                  VillagerInventoryType.WORK_TASK);
   }

   public void applyNbt(CompoundTag compoundTag, HolderLookup.Provider registryAccess) {
      read(compoundTag);
   }

   public CompoundTag toNbt(HolderLookup.Provider registryAccess) {
      CompoundTag tag = new CompoundTag();
      write(tag);
      return tag;
   }

   private void read(CompoundTag compoundTag) {
      String tagKey = FIELD_SAPLING;
      if (compoundTag.contains(tagKey))
         sapling = ItemStack.parseOptional(building.getRegistryAccess(), compoundTag.getCompound(tagKey));
      else
         sapling = DEFAULT_SAPLING;
   }

   private void write(CompoundTag tag) {
      if (sapling.isEmpty())
         sapling = DEFAULT_SAPLING;

      tag.put(FIELD_SAPLING, sapling.save(building.getRegistryAccess()));
   }

   public boolean isCorrectSapling(ItemStack stack) {
      return ItemStack.isSameItem(sapling, stack);
   }

  public static boolean isPlantableSapling(ItemStack stack) {
      return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SaplingBlock;
   }

   @Override
   public int getEyeDropperSlotCount() {
      return 1;
   }

   @Override
   public ItemStack getEyeDropperSlotItem(int slot) {
      return sapling;
   }

   @Override
   public boolean isValidEyeDropperSlotItem(int slot, ItemStack stack) {
      return stack.isEmpty() || isPlantableSapling(stack);
   }

   @Override
   public void setEyeDropperSlotItem(int slot, ItemStack stack) {
      // clearing the slot/reading the state for the very first time reverts back to the default sapling
      sapling = stack.isEmpty() ? DEFAULT_SAPLING : stack;
   }
}
