package com.uncreated.civilized.ui.menu.building.townhall;

import java.util.List;

import com.uncreated.civilized.ui.components.buttons.buildingtab.HomeTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.PermissionsTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.SettingsTabButton;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;
import com.uncreated.civilized.ui.menu.building.BuildingSettingsTab;
import com.uncreated.civilized.ui.menu.building.townhall.tabs.ManagePermissionsTab;
import com.uncreated.civilized.ui.menu.building.townhall.tabs.TownHallInfoTab;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TownHallBuildingScreen extends ABuildingMenuScreen {

   public TownHallBuildingScreen(BuildingMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
   }

   @Override
   protected ATab createDefaultTab(ITabHost tabHost) {
      return new TownHallInfoTab(tabHost, font, context);
   }

   @Override
   public List<Button> createTabButtons(ITabHost tabHost) {
      return List.of(
            new HomeTabButton(tabHost, 0, this::createDefaultTab),
            new PermissionsTabButton(tabHost, 1, host -> new ManagePermissionsTab(host, font, context)),
            new SettingsTabButton(tabHost, 2, host -> new BuildingSettingsTab(host, font, context)));
   }
}
