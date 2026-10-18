package com.uncreated.civilized.ui.menu.building.military.tabs;

import java.util.List;

import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

public class ManageSoldierTab extends ABuildingScreenTab {

   private static final int BUTTON_WIDTH = 60;
   private static final int BUTTON_HEIGHT = 18;

   private final Button backButton;

   public ManageSoldierTab(ITabHost tabHost, Font font, BuildingScreenContext context, VillagerInfo soldier) {
      super(
            tabHost,
            font,
            Component.translatable("menu.building.military.soldier.tab.heading", soldier.getFullName()),
            context);

      backButton =
            Button.builder(
                  Component.translatable("gui.misc.button.back"),
                  button -> tabHost.changeTab(new ManageSoldiersTab(tabHost, font, context)))
                  .pos(getX(), getBottom() - BUTTON_HEIGHT)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);
      graphics.drawWordWrap(
            font,
            Component.translatable("menu.building.military.soldier.placeholder"),
            getX(),
            getY() + 15,
            width,
            Colors.MENU_TEXT_DARK,
            false);
      backButton.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of(backButton);
   }
}
