package com.uncreated.civilized.ui.menu.building.worksite.cropfarm;

import java.util.List;
import java.util.function.Function;

import com.uncreated.civilized.ui.components.buttons.buildingtab.ManageWorkersTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.SettingsTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.WorksiteHomeTabButton;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;
import com.uncreated.civilized.ui.menu.building.BuildingSettingsTab;
import com.uncreated.civilized.ui.menu.building.worksite.cropfarm.tabs.CropFarmInfoTab;
import com.uncreated.civilized.ui.menu.building.worksite.cropfarm.tabs.CropsTab;
import com.uncreated.civilized.ui.menu.building.worksite.tabs.ManageWorkersTab;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CropFarmBuildingScreen extends ABuildingMenuScreen {

   private final Function<ITabHost, ATab> infoTab =
         tabHost -> new CropFarmInfoTab(tabHost, this.font, context, t -> new CropsTab(t, this.font, context, this));

   public CropFarmBuildingScreen(BuildingMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
   }

   @Override
   protected ATab createDefaultTab(ITabHost tabHost) {
      return infoTab.apply(tabHost);
   }

   @Override
   public List<Button> createTabButtons(ITabHost tabHost) {
      return List.of(
            new WorksiteHomeTabButton(tabHost, 0, infoTab),
            new ManageWorkersTabButton(tabHost, 1, host -> new ManageWorkersTab(host, font, context)),
            new SettingsTabButton(tabHost, 2, host -> new BuildingSettingsTab(host, font, context)));
   }
}
