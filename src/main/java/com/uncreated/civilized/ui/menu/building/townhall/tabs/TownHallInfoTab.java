package com.uncreated.civilized.ui.menu.building.townhall.tabs;

import java.util.List;

import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.menu.building.IBuildingInfoTab;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

public class TownHallInfoTab extends ABuildingScreenTab implements IBuildingInfoTab {

   private int population;

   public TownHallInfoTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      super(tabHost, font, Component.translatable("menu.building.residence.info.tab.heading"), context);
      refresh();
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);

      graphics.drawString(
            font,
            context.settlement().displayNameTranslation(),
            getX(),
            getY() + 20,
            Colors.MENU_TEXT_DARK,
            false);
      graphics.drawString(
            font,
            Component.translatable("menu.building.town_hall.level", context.building().getUpgradeLevel()),
            getX() + 8,
            getY() + 35,
            Colors.MENU_TEXT_DARK,
            false);
      graphics.drawString(
            font,
            Component.translatable("menu.building.town_hall.population.count", population),
            getX() + 8,
            getY() + 45,
            Colors.MENU_TEXT_DARK,
            false);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of();
   }

   @Override
   public void refresh() {
      population = ClientVillagerStore.INSTANCE.getCitizens(context.settlement().getSettlementId()).size();
   }
}
