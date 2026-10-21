package com.uncreated.civilized.ui.menu.building.occupants;

import java.util.Comparator;
import java.util.List;

import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;


public class ManageGarrisonTab extends ManageResidentsTab {

   public ManageGarrisonTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      super(tabHost, font, Component.translatable("menu.building.occupants.residents.tab.heading"), context);
      refresh();
   }

   @Override
   protected Component getSubHeading(List<VillagerInfo> residents) {
      return Component.translatable("menu.building.occupants.garrison.heading", residents.size());
   }

   @Override
   protected Component getEmptyOccupantsMessage() {
      return Component.translatable("menu.building.occupants.garrison.empty");
   }
}
