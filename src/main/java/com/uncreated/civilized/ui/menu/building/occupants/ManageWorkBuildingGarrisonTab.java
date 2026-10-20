package com.uncreated.civilized.ui.menu.building.occupants;

import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class ManageWorkBuildingGarrisonTab extends ManageWorkBuildingWorkersTab {

   public ManageWorkBuildingGarrisonTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      super(tabHost, font, Component.translatable("menu.building.occupants.garrison.tab.heading"), context);
   }

   @Override
   protected String workforceName() {
      return "garrison";
   }
}
