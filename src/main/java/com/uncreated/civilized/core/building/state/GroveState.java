package com.uncreated.civilized.core.building.state;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.logistics.hauling.VillagerInventoryType;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.InventoryStockRequirement;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.ui.menu.building.item.management.ItemManagementMenu;
import com.uncreated.civilized.ui.menu.building.worksite.grove.items.ChooseSaplingsMenu;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class GroveState extends BuildingState implements IItemManagementMenuSupplier {

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

   @Override
   public ItemManagementMenu createItemManagementMenu(
         Integer containerId,
         Inventory playerInventory,
         Building building,
         Settlement settlement) {
      return new ChooseSaplingsMenu(containerId, playerInventory, new SimpleContainer(1), settlement, building);
   }
}
