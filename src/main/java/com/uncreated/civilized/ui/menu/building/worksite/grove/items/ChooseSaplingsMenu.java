package com.uncreated.civilized.ui.menu.building.worksite.grove.items;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.state.GroveState;
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

public class ChooseSaplingsMenu extends ItemManagementMenu {

   // client constructor
   public ChooseSaplingsMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraDataFromServer) {
      this(
            containerId,
            playerInventory,
            new SimpleContainer(extraDataFromServer.readInt()),
            ClientSettlementsStore.INSTANCE.get(extraDataFromServer.readUUID()),
            ClientBuildingStore.INSTANCE.get(extraDataFromServer.readUUID()));
   }

   public ChooseSaplingsMenu(
         int containerId,
         Inventory playerInventory,
         Container container,
         Settlement settlement,
         Building building) {
      super(GuiRegistry.CHOOSE_SAPLINGS_MENU.get(), containerId);
      this.container = container;
      this.settlement = settlement;
      this.building = building;

      this.addSlot(new SaplingLegacyEyedropperSlot(container, 0, 123 + 2 * 18, 52));

      GroveState groveBehaviour = (GroveState) building.getState();
      this.container.setItem(0, groveBehaviour.getSapling());

      this.addStandardInventorySlots(playerInventory, 87, 88);

      this.container.startOpen(playerInventory.player);
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      this.container.stopOpen(player);

      if (!(player instanceof ServerPlayer serverPlayer))
         return;

      if (!(building.getState() instanceof GroveState groveBehaviour))
         return;

      groveBehaviour.setSapling(container.getItem(0));

      ServerBuildingsStore.INSTANCE.replicateChange(building, StoreOperation.UPDATE);
      ServerBuildingsStore.INSTANCE.setDirty();

      // BAD IMPLEMENTATION: this method is called when the player's menu closes (e.g. when escape is pressed), so this
      // currently re-opens the UI when it shouldn't.
      CompoundTag additionalData = new CompoundTag();
      building.getState().serverAddToBuildingScreenContext(additionalData, serverPlayer.serverLevel());
      PacketDistributor.sendToPlayer(serverPlayer, new ShowBuildingScreen(building.getBuildingId(), additionalData));
   }
}
