package com.uncreated.civilized.ui.menu.building;

import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ATabWithViewableItemSlots;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public abstract class ABuildingScreenTab extends ATabWithViewableItemSlots {

   private final Component tabTitle;
   protected final BuildingScreenContext context;

   public ABuildingScreenTab(
         ITabHost tabHost,
         Font font,
         Component tabTitle,
         BuildingScreenContext context) {
      super(tabHost.getTabCoords(), font);
      this.tabTitle = tabTitle;
      this.context = context;
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      int fontStartX = (width - font.width(tabTitle)) / 2;
      graphics.drawWordWrap(
            font,
            tabTitle,
            getX() + fontStartX,
            getY(),
            width - fontStartX,
            Colors.MENU_TEXT_DARK,
            false);
   }

}
