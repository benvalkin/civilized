package com.uncreated.civilized.ui.menu.building.worksite.tabs;

import java.util.List;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.residence.tabs.ManageResidentsTab;
import com.uncreated.civilized.ui.menu.building.widgets.ManageOccupantWidget;
import com.uncreated.civilized.ui.menu.building.widgets.ManageWorkerWidget;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class ManageWorkersTab extends ManageResidentsTab {

   public static final int MAX_ASSIGNED_WORKERS = 1;

   public ManageWorkersTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      super(
            tabHost,
            font,
            Component.translatable("menu.building.worksite.workers.tab.heading"),
            context);
   }

   @Override
   protected int getMaxNumberOfOccupants() {
      return MAX_ASSIGNED_WORKERS;
   }

   @Override
   protected Component getSubHeading(List<VillagerInfo> currentOccupants) {
      return Component
            .translatable("menu.building.worksite.workers.heading", currentOccupants.size(), MAX_ASSIGNED_WORKERS);
   }

   @Override
   protected Component getSubHeadingNoCandidates() {
      return Component.translatable("menu.building.worksite.workers.heading.empty");
   }

   @Override
   protected List<VillagerInfo> getCurrentOccupants(Building building, Settlement settlement) {
      return BuildingUtil.getAssignedWorkers(building, ClientVillagerStore.INSTANCE);
   }

   @Override
   protected List<VillagerInfo> getCandidateOccupants(Building building, Settlement settlement) {
      return super.getCandidateOccupants(building, settlement).stream()
            .filter(v -> v.getOccupation() == building.getBuildingType().occupation())
            .toList();
   }

   @Override
   protected ManageOccupantWidget.ManagementOption getAssignButtonAction(VillagerInfo villager) {
      if (villager.isAssignedWorkerOf(context.building()))
         return ManageOccupantWidget.ManagementOption.UNASSIGN;
      else
         return ManageOccupantWidget.ManagementOption.ASSIGN;
   }

   protected ManageOccupantWidget createManagementWidget(
         int x,
         int y,
         int width,
         int height,
         VillagerInfo villagerInfo,
         ManageOccupantWidget.ManagementOption managementOption,
         boolean isBuildingFull) {
      return new ManageWorkerWidget(
            x,
            y,
            width,
            height,
            font,
            context.building(),
            villagerInfo,
            managementOption,
            isBuildingFull);
   }
}
