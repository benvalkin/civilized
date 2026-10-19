package com.uncreated.civilized.ui.menu.building.occupants;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.networking.packets.AssignWorkerRequest;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ManageWorkersTab extends AManageOccupantsTab {

   public ManageWorkersTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      this(tabHost, font, Component.translatable("menu.building.occupants.workers.tab.heading"), context);
   }

   protected ManageWorkersTab(ITabHost tabHost, Font font, Component heading, BuildingScreenContext context) {
      super(tabHost, font, heading, context);
      refresh();
   }

   protected String workforceName() {
      return "workers";
   }

   @Override
   protected List<VillagerInfo> provideOccupantsModelData() {
      // todo: let any adult work, once spouses are folded into the homemaker occupation
      return ClientVillagerStore.INSTANCE.getCitizens(building.getSettlementId())
            .stream()
            .filter(v -> v.getNpcRole().is(VillagerNpcRoles.WORKER))
            // this building's workers first, then villagers without a job, since giving them one costs nothing
            .sorted(
                  Comparator.comparing((VillagerInfo v) -> !v.isAssignedWorkerOf(building))
                        .thenComparing(v -> !v.getOccupation().is(VillagerOccupations.UNEMPLOYED))
                        .thenComparing(VillagerInfo::getFullName))
            .toList();
   }

   @Override
   protected OccupantRowWidget buildElementWidgetFromModel(VillagerInfo villager, int x, int y, int width) {
      Button button;
      if (villager.isAssignedWorkerOf(building))
         button = createUnassignButton(villager);
      else
         button = createAssignButton(villager);
      return new OccupantRowWidget(x, y, width, font, villager, button);
   }

   private Button createUnassignButton(VillagerInfo worker) {
      return Button
            .builder(
                  Component.translatable("menu.building.occupants.button.unassign." + workforceName()),
                  button -> PacketDistributor
                        .sendToServer(new AssignWorkerRequest(worker.getVillagerId(), Optional.empty())))
            .build();
   }

   private Button createAssignButton(VillagerInfo villager) {
      Button button =
            Button.builder(
                  Component.translatable("menu.building.occupants.button.assign"),
                  b -> PacketDistributor.sendToServer(
                        new AssignWorkerRequest(villager.getVillagerId(), Optional.of(building.getBuildingId()))))
                  .build();

      if (!BuildingUtil.worksiteHasFreeWorkerSlot(building, ClientVillagerStore.INSTANCE)) {
         button.active = false;
         button.setTooltip(Tooltip.create(Component.translatable("menu.building.occupants.button.assign.full")));
      } else if (!villager.getOccupation().is(VillagerOccupations.UNEMPLOYED)) {
         button.setTooltip(
               Tooltip.create(
                     Component.translatable(
                           "menu.building.occupants.assign.leaves_job",
                           villager.getFullName(),
                           villager.getOccupation().translation())));
      }
      return button;
   }

   @Override
   protected Component getSubHeading(List<VillagerInfo> villagers) {
      long workers = villagers.stream().filter(v -> v.isAssignedWorkerOf(building)).count();
      return Component.translatable(
            "menu.building.occupants." + workforceName() + ".heading",
            workers,
            building.getBuildingType().maxWorkers());
   }

   @Override
   protected Component getEmptyOccupantsMessage() {
      return Component.translatable("menu.building.occupants." + workforceName() + ".empty");
   }
}
