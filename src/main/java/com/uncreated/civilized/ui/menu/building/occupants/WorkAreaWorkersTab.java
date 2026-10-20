package com.uncreated.civilized.ui.menu.building.occupants;

import java.util.Comparator;
import java.util.List;

import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.building.util.WorkAreaUtil;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.networking.packets.AssignWorkAreaRequest;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class WorkAreaWorkersTab extends AManageOccupantsTab {

   public WorkAreaWorkersTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      super(tabHost, font, Component.translatable("menu.building.occupants.work_area.tab.heading"), context);
      refresh();
   }

   private boolean worksHere(VillagerInfo villager) {
      return WorkAreaUtil.findWorkArea(villager, ClientBuildingStore.INSTANCE)
            .filter(area -> area.getBuildingId().equals(building.getBuildingId()))
            .isPresent();
   }

   @Override
   protected List<VillagerInfo> provideOccupantsModelData() {
      return ClientVillagerStore.INSTANCE.getCitizens(building.getSettlementId())
            .stream()
            .filter(v -> WorkAreaUtil.canWork(v, building))
            // list this work area's currently assigned workers first
            .sorted(
                  Comparator.comparing((VillagerInfo v) -> !worksHere(v))
                        .thenComparing(v -> WorkAreaUtil.findWorkArea(v, ClientBuildingStore.INSTANCE).isPresent())
                        .thenComparing(VillagerInfo::getFullName))
            .toList();
   }

   @Override
   protected OccupantRowWidget buildElementWidgetFromModel(VillagerInfo villager, int x, int y, int width) {
      if (worksHere(villager))
         return new OccupantRowWidget(x, y, width, font, villager, null);

      Button assignButton =
            Button.builder(
                  Component.translatable("menu.building.occupants.button.assign"),
                  button -> PacketDistributor
                        .sendToServer(new AssignWorkAreaRequest(villager.getVillagerId(), building.getBuildingId())))
                  .build();

      List<VillagerInfo> workers = BuildingUtil.getWorkers(building, ClientVillagerStore.INSTANCE);
      if (!workers.isEmpty() && workers.size() >= building.getBuildingType().maxWorkers())
         assignButton.setTooltip(
               Tooltip.create(
                     Component.translatable(
                           "menu.building.occupants.work_area.assign.swaps",
                           villager.getFullName(),
                           workers.getFirst().getFullName())));

      return new OccupantRowWidget(x, y, width, font, villager, assignButton);
   }

   @Override
   protected Component getSubHeading(List<VillagerInfo> villagers) {
      long workers = villagers.stream().filter(this::worksHere).count();
      return Component
            .translatable("menu.building.occupants.workers.heading", workers, building.getBuildingType().maxWorkers());
   }

   @Override
   protected Component getEmptyOccupantsMessage() {
      return Component.translatable(
            "menu.building.occupants.work_area.empty",
              building.getBuildingType().occupation().translation());
   }
}
