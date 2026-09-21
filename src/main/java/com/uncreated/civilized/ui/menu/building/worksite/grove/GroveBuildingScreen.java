package com.uncreated.civilized.ui.menu.building.worksite.grove;

import java.util.List;

import com.uncreated.civilized.ui.components.buttons.buildingtab.ManageWorkersTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.SettingsTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.WorksiteHomeTabButton;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;
import com.uncreated.civilized.ui.menu.building.BuildingSettingsTab;
import com.uncreated.civilized.ui.menu.building.worksite.grove.tabs.GroveInfoTab;
import com.uncreated.civilized.ui.menu.building.worksite.grove.tabs.SaplingTab;
import com.uncreated.civilized.ui.menu.building.worksite.tabs.ManageWorkersTab;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GroveBuildingScreen extends ABuildingMenuScreen {

   public GroveBuildingScreen(BuildingMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
   }

   @Override
   protected ATab createDefaultTab(ITabHost tabHost) {
      return new GroveInfoTab(tabHost, this.font, context, t -> new SaplingTab(t, this.font, context, this));
   }

   @Override
   public List<Button> createTabButtons(ITabHost tabHost) {
      return List.of(
            new WorksiteHomeTabButton(tabHost, 0, this::createDefaultTab),
            new ManageWorkersTabButton(tabHost, 1, host -> new ManageWorkersTab(host, font, context)),
            new SettingsTabButton(tabHost, 2, host -> new BuildingSettingsTab(host, font, context)));
   }
}
