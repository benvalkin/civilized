package com.uncreated.civilized.ui.menu.villager.tabs;

import com.uncreated.civilized.ui.menu.villager.VillagerScreen;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.TabCoords;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public abstract class AVillagerPanelTab extends ATab {

   protected static final int PADDING = 8;

   public AVillagerPanelTab(TabCoords tabCoords, Font font) {
      super(tabCoords, font);
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(getX(), getY(), getRight(), getBottom(), VillagerScreen.PANEL_COLOUR);
   }
}
