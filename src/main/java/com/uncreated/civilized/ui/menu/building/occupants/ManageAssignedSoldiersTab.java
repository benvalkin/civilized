package com.uncreated.civilized.ui.menu.building.occupants;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ManageAssignedSoldiersTab extends ManageWorkBuildingWorkersTab {

   public ManageAssignedSoldiersTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      super(tabHost, font, Component.translatable("menu.building.occupants.assigned_soldiers.tab.heading"), context);
   }

   @Override
   protected @NotNull Component getAssignedButtonText() {
      return Component.translatable("menu.building.occupants.button.assign.assigned_soldiers");
   }

   private @NotNull MutableComponent getUnassignButtonText() {
      return Component.translatable("menu.building.occupants.button.unassign.assigned_soldiers");
   }

   @Override
   protected Component getSubHeading(List<VillagerInfo> villagers) {
      long workers = villagers.stream().filter(v -> v.isAssignedWorkerOf(building)).count();
      return Component
            .translatable("menu.building.occupants.assigned_soldiers.heading", workers, building.getMaxWorkers());
   }

   @Override
   protected Component getEmptyOccupantsMessage() {
      return Component.translatable("menu.building.occupants.assigned_soldiers.empty");
   }
}
