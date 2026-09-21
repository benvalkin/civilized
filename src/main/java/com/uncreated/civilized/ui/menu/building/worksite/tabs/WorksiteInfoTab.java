package com.uncreated.civilized.ui.menu.building.worksite.tabs;

import java.util.List;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

public class WorksiteInfoTab extends ABuildingScreenTab {

   private List<VillagerInfo> workers;

   public WorksiteInfoTab(
         ITabHost tabHost,
         Font font,
         BuildingScreenContext context) {
      super(
            tabHost,
            font,
            Component.translatable("menu.building.residence.info.tab.heading"),
            context);
      this.workers = createOccupantsList();
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);

      graphics.drawString(
            font,
            Component.translatable(
                  "menu.building.worksite.workers.heading",
                  workers.size(),
                  ManageWorkersTab.MAX_ASSIGNED_WORKERS),
            getX(),
            getY() + 20,
            Colors.MENU_TEXT_DARK,
            false);

      for (int i = 0; i < workers.size(); i++) {

         VillagerInfo occupant = workers.get(i);
         graphics.drawString(
               font,
               Component.literal(occupant.getFullName()),
               getX() + 8,
               getY() + 35 + i * 10,
               Colors.MENU_TEXT_DARK,
               false);
      }
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of();
   }

   private List<VillagerInfo> createOccupantsList() {
      return BuildingUtil.getAssignedWorkers(context.building(), ClientVillagerStore.INSTANCE);
   }

   @Override
   public void refresh() {
      this.workers = createOccupantsList();
   }
}
