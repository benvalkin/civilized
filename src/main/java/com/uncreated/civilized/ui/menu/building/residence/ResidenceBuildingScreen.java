package com.uncreated.civilized.ui.menu.building.residence;

import java.util.ArrayList;
import java.util.List;

import com.uncreated.civilized.ui.components.buttons.buildingtab.HomeTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.ManageResidentsTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.ManageWorkersTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.SettingsTabButton;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;
import com.uncreated.civilized.ui.menu.building.BuildingSettingsTab;
import com.uncreated.civilized.ui.menu.building.occupants.ManageResidentsTab;
import com.uncreated.civilized.ui.menu.building.occupants.ManageWorkBuildingWorkersTab;
import com.uncreated.civilized.ui.menu.building.residence.tabs.ResidenceInfoTab;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ResidenceBuildingScreen extends ABuildingMenuScreen {

   public ResidenceBuildingScreen(BuildingMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
   }

   @Override
   protected ATab createDefaultTab(ITabHost tabHost) {
      return new ResidenceInfoTab(tabHost, font, context);
   }

   @Override
   public List<Button> createTabButtons(ITabHost tabHost) {
      List<Button> buttons = new ArrayList<>();
      buttons.add(new HomeTabButton(tabHost, buttons.size(), this::createDefaultTab));
      if (context.building().getBuildingType().isWorkBuilding())
         buttons.add(
               new ManageWorkersTabButton(tabHost, buttons.size(), host -> new ManageWorkBuildingWorkersTab(host, font, context)));
      buttons.add(
            new ManageResidentsTabButton(tabHost, buttons.size(), host -> new ManageResidentsTab(host, font, context)));
      buttons.add(new SettingsTabButton(tabHost, buttons.size(), host -> new BuildingSettingsTab(host, font, context)));
      return buttons;
   }
}
