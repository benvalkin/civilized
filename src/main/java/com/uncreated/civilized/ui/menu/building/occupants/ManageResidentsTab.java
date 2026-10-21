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


public class ManageResidentsTab extends ManageOccupantsTab {

   public ManageResidentsTab(ITabHost tabHost, Font font, Component component, BuildingScreenContext context) {
      super(tabHost, font, component, context);
      refresh();
   }

   public ManageResidentsTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      this(tabHost, font, Component.translatable("menu.building.occupants.residents.tab.heading"), context);
   }

   @Override
   protected List<VillagerInfo> provideOccupantsModelData() {
      return BuildingUtil.getOccupants(building, ClientVillagerStore.INSTANCE)
            .stream()
            .sorted(Comparator.comparing(VillagerInfo::getFullName))
            .toList();
   }

   @Override
   protected OccupantRowWidget buildElementWidgetFromModel(VillagerInfo resident, int x, int y, int width) {
      return new OccupantRowWidget(x, y, width, font, resident, null);
   }

   @Override
   protected Component getSubHeading(List<VillagerInfo> residents) {
      return Component.translatable("menu.building.occupants.residents.heading", residents.size());
   }

   @Override
   protected Component getEmptyOccupantsMessage() {
      return Component.translatable("menu.building.occupants.residents.empty");
   }
}
