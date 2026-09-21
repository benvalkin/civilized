package com.uncreated.civilized.ui.menu.building.inn.tabs;

import java.util.List;

import com.uncreated.civilized.core.building.util.BuildingUtil;
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

public class InnVisitorsTab extends ABuildingScreenTab {

   private List<VillagerInfo> visitors;

   public InnVisitorsTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      super(tabHost, font, Component.literal("Visitors"), context);
      this.visitors = createVisitorsList();
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);
      if (visitors.isEmpty()) {
         graphics.drawWordWrap(
               font,
               Component.translatable("menu.building.inn.visitors.count.heading.empty"),
               getX(),
               getY() + 20,
               width,
               Colors.MENU_TEXT_DARK,
               false);
         return;
      }

      graphics.drawString(
            font,
            Component.translatable("menu.building.inn.visitors.count.heading"),
            getX(),
            getY() + 20,
            Colors.MENU_TEXT_DARK,
            false);

      for (int i = 0; i < visitors.size(); i++) {

         VillagerInfo visitor = visitors.get(i);
         graphics.drawString(
               font,
               Component.literal(visitor.getFullName()),
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

   private List<VillagerInfo> createVisitorsList() {
      return BuildingUtil.getResidents(context.building(), ClientVillagerStore.INSTANCE);
   }

   @Override
   public void refresh() {
      visitors = createVisitorsList();
   }
}
