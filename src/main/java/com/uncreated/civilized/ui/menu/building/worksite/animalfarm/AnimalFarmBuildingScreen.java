package com.uncreated.civilized.ui.menu.building.worksite.animalfarm;

import java.util.List;

import com.uncreated.civilized.ui.components.buttons.buildingtab.AnimalFoodTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.ManageWorkersTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.SettingsTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.WorksiteHomeTabButton;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;
import com.uncreated.civilized.ui.menu.building.BuildingSettingsTab;
import com.uncreated.civilized.ui.menu.building.worksite.tabs.ManageWorkersTab;
import com.uncreated.civilized.ui.menu.building.worksite.animalfarm.tabs.AnimalFarmInfoTab;
import com.uncreated.civilized.ui.menu.building.worksite.animalfarm.tabs.AnimalFoodTab;
import com.uncreated.civilized.ui.tabs.ATab;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;


public class AnimalFarmBuildingScreen extends ABuildingMenuScreen {

   public AnimalFarmBuildingScreen(BuildingMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
   }

   @Override
   protected List<ATab> createTabs(int contentLeftPos, int contentTopPos, int tabWidth, int tabHeight) {
      return List.of(
            new AnimalFarmInfoTab(0, contentLeftPos, contentTopPos, tabWidth, tabHeight, this.font, context),
            new AnimalFoodTab(1, contentLeftPos, contentTopPos, tabWidth, tabHeight, this.font, context, this),
            new ManageWorkersTab(2, contentLeftPos, contentTopPos, tabWidth, tabHeight, this.font, context),
            new BuildingSettingsTab(3, contentLeftPos, contentTopPos, tabWidth, tabHeight, this.font, context));
   }

   @Override
   public List<Button> createTabButtons() {
      return List.of(
            new WorksiteHomeTabButton(this, 0),
            new AnimalFoodTabButton(this, 1),
            new ManageWorkersTabButton(this, 2),
            new SettingsTabButton(this, 3));
   }
}
