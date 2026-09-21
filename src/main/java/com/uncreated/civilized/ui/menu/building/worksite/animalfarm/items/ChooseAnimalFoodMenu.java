package com.uncreated.civilized.ui.menu.building.worksite.animalfarm.items;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingScreenOpener;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.state.animalfarm.AnimalFarmState;
import com.uncreated.civilized.core.settlement.ClientSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.neoforge.registration.gui.GuiRegistry;
import com.uncreated.civilized.ui.menu.building.item.management.ItemManagementMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class ChooseAnimalFoodMenu extends ItemManagementMenu {

   // client constructor
   public ChooseAnimalFoodMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraDataFromServer) {
      this(
            containerId,
            playerInventory,
            new SimpleContainer(extraDataFromServer.readInt()),
            ClientSettlementsStore.INSTANCE.get(extraDataFromServer.readUUID()),
            ClientBuildingStore.INSTANCE.get(extraDataFromServer.readUUID()));
   }

   public ChooseAnimalFoodMenu(
         int containerId,
         Inventory playerInventory,
         Container container,
         Settlement settlement,
         Building building) {
      super(GuiRegistry.CHOOSE_ANIMAL_FOOD_MENU.get(), containerId);
      this.container = container;
      this.settlement = settlement;
      this.building = building;

      AnimalFarmState buildingState = (AnimalFarmState) building.getState();

      this.addSlot(new AnimalFoodEyedropperSlot(buildingState, container, 0, 123, 52));
      this.addSlot(new AnimalFoodEyedropperSlot(buildingState, container, 1, 123 + 2 * 18, 52));
      this.addSlot(new AnimalFoodEyedropperSlot(buildingState, container, 2, 123 + 4 * 18, 52));

      AnimalFarmState animalFarmBehaviour = (AnimalFarmState) building.getState();
      animalFarmBehaviour.tryApplyDefaults();
      this.container.setItem(0, animalFarmBehaviour.getFoodSlot(0));
      this.container.setItem(1, animalFarmBehaviour.getFoodSlot(1));
      this.container.setItem(2, animalFarmBehaviour.getFoodSlot(2));

      this.addStandardInventorySlots(playerInventory, 87, 88);

      this.container.startOpen(playerInventory.player);
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      this.container.stopOpen(player);

      if (!(player instanceof ServerPlayer serverPlayer))
         return;

      if (!(building.getState() instanceof AnimalFarmState animalFarmBehaviour))
         return;

      animalFarmBehaviour.setFoodSlot(0, container.getItem(0));
      animalFarmBehaviour.setFoodSlot(1, container.getItem(1));
      animalFarmBehaviour.setFoodSlot(2, container.getItem(2));

      ServerBuildingsStore.INSTANCE.replicateChange(building, StoreOperation.UPDATE);
      ServerBuildingsStore.INSTANCE.setDirty();

      // BAD IMPLEMENTATION: this method is called when the player's menu closes (e.g. when escape is pressed), so this
      // currently re-opens the UI when it shouldn't.
      BuildingScreenOpener.open(serverPlayer, building);
   }
}
