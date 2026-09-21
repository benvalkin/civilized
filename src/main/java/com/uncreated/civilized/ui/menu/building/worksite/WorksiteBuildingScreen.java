package com.uncreated.civilized.ui.menu.building.worksite;

import java.util.List;

import com.uncreated.civilized.ui.components.buttons.buildingtab.ManageWorkersTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.SettingsTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.WorksiteHomeTabButton;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreen;
import com.uncreated.civilized.ui.menu.building.BuildingSettingsTab;
import com.uncreated.civilized.ui.menu.building.worksite.tabs.WorksiteInfoTab;
import com.uncreated.civilized.ui.menu.building.worksite.tabs.ManageWorkersTab;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class WorksiteBuildingScreen extends ABuildingScreen {

   public WorksiteBuildingScreen(BuildingScreenContext context, Component title) {
      super(context, title);
   }

   @Override
   protected ATab createDefaultTab(ITabHost tabHost) {
      return new WorksiteInfoTab(tabHost, font, context);
   }

   @Override
   public List<Button> createTabButtons(ITabHost tabHost) {
      return List.of(
            new WorksiteHomeTabButton(tabHost, 0, this::createDefaultTab),
            new ManageWorkersTabButton(tabHost, 1, host -> new ManageWorkersTab(host, font, context)),
            new SettingsTabButton(tabHost, 2, host -> new BuildingSettingsTab(host, font, context)));
   }
}
