package com.uncreated.civilized.ui.menu.building.military;

import java.util.List;

import com.uncreated.civilized.ui.components.buttons.buildingtab.ManageGarrisonTabButton;
import com.uncreated.civilized.ui.menu.building.occupants.ManageGarrisonTab;
import com.uncreated.civilized.ui.menu.building.occupants.ManageAssignedSoldiersTab;
import com.uncreated.civilized.ui.components.buttons.buildingtab.ManageWorkersTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.HomeTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.ManageSoldiersTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.SettingsTabButton;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;
import com.uncreated.civilized.ui.menu.building.BuildingSettingsTab;
import com.uncreated.civilized.ui.menu.building.military.tabs.ManageSoldiersTab;
import com.uncreated.civilized.ui.menu.building.residence.tabs.ResidenceInfoTab;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MilitaryBuildingScreen extends ABuildingMenuScreen {

   public MilitaryBuildingScreen(BuildingMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
   }

   @Override
   protected ATab createDefaultTab(ITabHost tabHost) {
      return new ResidenceInfoTab(tabHost, font, context);
   }

   @Override
   public List<Button> createTabButtons(ITabHost tabHost) {
      return List.of(
            new HomeTabButton(tabHost, 0, this::createDefaultTab),
            new ManageWorkersTabButton(
                  tabHost,
                  1,
                  host -> new ManageAssignedSoldiersTab(host, font, context),
                  Tooltip.create(Component.translatable("menu.building.occupants.assigned_soldiers.tab.heading"))),
            new ManageGarrisonTabButton(tabHost, 2, host -> new ManageGarrisonTab(host, font, context)),
            new ManageSoldiersTabButton(tabHost, 3, host -> new ManageSoldiersTab(host, font, context)),
            new SettingsTabButton(tabHost, 4, host -> new BuildingSettingsTab(host, font, context)));
   }
}
