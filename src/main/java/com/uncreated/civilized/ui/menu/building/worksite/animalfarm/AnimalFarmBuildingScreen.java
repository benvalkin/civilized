package com.uncreated.civilized.ui.menu.building.worksite.animalfarm;

import java.util.List;
import java.util.function.Function;

import com.uncreated.civilized.ui.components.buttons.buildingtab.AnimalFoodTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.LazyLoadWorksiteHomeTabButton;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;
import com.uncreated.civilized.ui.menu.building.worksite.animalfarm.tabs.AnimalFarmInfoTab;
import com.uncreated.civilized.ui.menu.building.worksite.animalfarm.tabs.AnimalFoodTab;
import com.uncreated.civilized.ui.menu.building.worksite.animalfarm.tabs.TabCoords;
import com.uncreated.civilized.ui.tabs.ATab;

import com.uncreated.civilized.ui.tabs.ILazyLoadTabHost;
import com.uncreated.civilized.ui.tabs.ITabHost;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AnimalFarmBuildingScreen extends ABuildingMenuScreen {

   public AnimalFarmBuildingScreen(BuildingMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
   }

   private final Function<ILazyLoadTabHost, ATab> foodTab =
         tabHost -> new AnimalFoodTab(tabHost, this.font, context, this);
   private final Function<ILazyLoadTabHost, ATab> infoTab =
         tabHost -> new AnimalFarmInfoTab(tabHost, this.font, context, foodTab);

   // new ManageWorkersTab(2, contentLeftPos, contentTopPos, tabWidth, tabHeight, this.font, context),
   // new BuildingSettingsTab(3, contentLeftPos, contentTopPos, tabWidth, tabHeight, this.font, context));

   @Override
   protected ATab createDefaultTab(ILazyLoadTabHost tabHost) {
      return infoTab.apply(tabHost);
   }

   @Override
   public List<Button> createTabButtons(ILazyLoadTabHost tabHost) {
      return List.of(
            new LazyLoadWorksiteHomeTabButton(this, 0, infoTab),
            new AnimalFoodTabButton(this, 1, foodTab)
      // new ManageWorkersTabButton(this, 2),
      // new SettingsTabButton(this, 3)
      );
   }
}
