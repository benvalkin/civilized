package com.uncreated.civilized.ui.menu.building.worksite.cropfarm.items;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.state.CropFarmState;
import com.uncreated.civilized.core.settlement.ClientSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.neoforge.registration.gui.GuiRegistry;
import com.uncreated.civilized.networking.packets.ShowBuildingScreen;
import com.uncreated.civilized.ui.menu.building.item.management.ItemManagementMenu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public class ChooseCropsMenu extends ItemManagementMenu {

   // client constructor
   public ChooseCropsMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraDataFromServer) {
      this(
            containerId,
            playerInventory,
            new SimpleContainer(extraDataFromServer.readInt()),
            ClientSettlementsStore.INSTANCE.get(extraDataFromServer.readUUID()),
            ClientBuildingStore.INSTANCE.get(extraDataFromServer.readUUID()));
   }

   public ChooseCropsMenu(
         int containerId,
         Inventory playerInventory,
         Container container,
         Settlement settlement,
         Building building) {
      super(GuiRegistry.CHOOSE_CROPS_MENU.get(), containerId);
      this.container = container;
      this.settlement = settlement;
      this.building = building;

      this.addSlot(new CropLegacyEyedropperSlot(container, 0, 123, 52));
      this.addSlot(new CropLegacyEyedropperSlot(container, 1, 123 + 2 * 18, 52));
      this.addSlot(new CropLegacyEyedropperSlot(container, 2, 123 + 4 * 18, 52));

      CropFarmState cropFarmBehaviour = (CropFarmState) building.getState();
      cropFarmBehaviour.tryApplyDefaults();
      this.container.setItem(0, cropFarmBehaviour.getCropSlot(0));
      this.container.setItem(1, cropFarmBehaviour.getCropSlot(1));
      this.container.setItem(2, cropFarmBehaviour.getCropSlot(2));

      this.addStandardInventorySlots(playerInventory, 87, 88);

      this.container.startOpen(playerInventory.player);
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      this.container.stopOpen(player);

      if (!(player instanceof ServerPlayer serverPlayer))
         return;

      if (!(building.getState() instanceof CropFarmState cropFarmBehaviour))
         return;

      cropFarmBehaviour.setCropSlot(0, container.getItem(0));
      cropFarmBehaviour.setCropSlot(1, container.getItem(1));
      cropFarmBehaviour.setCropSlot(2, container.getItem(2));

      ServerBuildingsStore.INSTANCE.replicateChange(building, StoreOperation.UPDATE);
      ServerBuildingsStore.INSTANCE.setDirty();

      // BAD IMPLEMENTATION: this method is called when the player's menu closes (e.g. when escape is pressed), so this
      // currently re-opens the UI when it shouldn't.
      CompoundTag additionalData = new CompoundTag();
      building.getState().serverAddToBuildingScreenContext(additionalData, serverPlayer.serverLevel());
      PacketDistributor.sendToPlayer(serverPlayer, new ShowBuildingScreen(building.getBuildingId(), additionalData));
   }
}
