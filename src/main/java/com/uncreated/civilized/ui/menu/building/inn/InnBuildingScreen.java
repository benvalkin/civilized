package com.uncreated.civilized.ui.menu.building.inn;

import java.util.List;

import com.uncreated.civilized.ui.components.buttons.buildingtab.HomeTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.ManageResidentsTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.SettingsTabButton;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;
import com.uncreated.civilized.ui.menu.building.BuildingSettingsTab;
import com.uncreated.civilized.ui.menu.building.inn.tabs.InnMainTab;
import com.uncreated.civilized.ui.menu.building.inn.tabs.InnVisitorsTab;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class InnBuildingScreen extends ABuildingMenuScreen {

   public InnBuildingScreen(BuildingMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
   }

   @Override
   protected ATab createDefaultTab(ITabHost tabHost) {
      return new InnMainTab(tabHost, font, context);
   }

   @Override
   public List<Button> createTabButtons(ITabHost tabHost) {
      return List.of(
            new HomeTabButton(tabHost, 0, this::createDefaultTab),
            new ManageResidentsTabButton(
                  tabHost,
                  1,
                  host -> new InnVisitorsTab(host, font, context),
                  Tooltip.create(Component.translatable("menu.building.inn.visitors.count.heading"))),
            new SettingsTabButton(tabHost, 2, host -> new BuildingSettingsTab(host, font, context)));
   }
}
