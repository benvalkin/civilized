package com.uncreated.civilized.networking.packets;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.state.artisan.ArtisanHouseState;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// todo: probably doesn't need to be it's own class

/**
 * The checks shared by the packets that edit a production bill from the building screen. The client is never trusted,
 * so every packet is checked against what the server knows before anything is looked up or saved.
 */
final class ProductionBillEditValidation {

   private ProductionBillEditValidation() {
   }

   record EditableArtisanHouse(Building building, ArtisanHouseState state) {
   }

   static Optional<EditableArtisanHouse> findArtisanHouseForRecipeValidation(
         UUID buildingId,
         ProductionType productionType,
         List<ItemStack> inputs,
         IPayloadContext context) {

      if (productionType == null)
         return Optional.empty();

      Optional<Building> building = ServerBuildingsStore.INSTANCE.find(buildingId);
      if (building.isEmpty())
         return Optional.empty();

      if (!(context.player().containerMenu instanceof BuildingMenu buildingMenu)
            || !buildingMenu.getBuilding().getBuildingId().equals(buildingId))
         return Optional.empty();

      if (!(building.get().getState() instanceof ArtisanHouseState artisanHouseState))
         return Optional.empty();

      if (artisanHouseState.tryGetProductionBills(productionType).isEmpty())
         return Optional.empty();

      if (inputs.size() != productionType.inputSlotCount())
         return Optional.empty();

      return Optional.of(new EditableArtisanHouse(building.get(), artisanHouseState));
   }

   /** One of each input, since a bill only records which item goes in each slot. */
   static List<ItemStack> singleItems(List<ItemStack> inputs) {
      return inputs.stream().map(stack -> stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1)).toList();
   }
}
