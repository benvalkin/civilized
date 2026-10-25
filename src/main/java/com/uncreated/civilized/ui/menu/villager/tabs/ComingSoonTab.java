package com.uncreated.civilized.ui.menu.villager.tabs;

import java.util.List;

import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.TabCoords;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

public abstract class ComingSoonTab extends ATab {

   private static final int PADDING = 8;

   private final Component name;

   public ComingSoonTab(TabCoords tabCoords, Font font, Component name) {
      super(tabCoords, font);
      this.name = name;
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      int x = getX() + PADDING;
      int y = getY() + PADDING;
      graphics.drawString(font, name, x, y, Colors.MENU_TEXT_VILLAGER_DIALOGUE);
      graphics.drawString(
            font,
            Component.translatable("menu.villager.tab.coming_soon"),
            x,
            y + font.lineHeight + 2,
            Colors.TEXT_LIGHT_MUTED);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of();
   }
}
