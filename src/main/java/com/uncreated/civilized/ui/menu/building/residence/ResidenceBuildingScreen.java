package com.uncreated.civilized.ui.menu.building.residence;

import java.util.List;

import com.uncreated.civilized.ui.components.buttons.buildingtab.HomeTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.ManageResidentsTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.SettingsTabButton;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreen;
import com.uncreated.civilized.ui.menu.building.BuildingSettingsTab;
import com.uncreated.civilized.ui.menu.building.residence.tabs.ManageResidentsTab;
import com.uncreated.civilized.ui.menu.building.residence.tabs.ResidenceInfoTab;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ResidenceBuildingScreen extends ABuildingScreen {

   public ResidenceBuildingScreen(BuildingScreenContext context, Component title) {
      super(context, title);
   }

   @Override
   protected ATab createDefaultTab(ITabHost tabHost) {
      return new ResidenceInfoTab(tabHost, font, context);
   }

   @Override
   public List<Button> createTabButtons(ITabHost tabHost) {
      return List.of(
            new HomeTabButton(tabHost, 0, this::createDefaultTab),
            new ManageResidentsTabButton(tabHost, 1, host -> new ManageResidentsTab(host, font, context)),
            new SettingsTabButton(tabHost, 2, host -> new BuildingSettingsTab(host, font, context)));
   }
}
