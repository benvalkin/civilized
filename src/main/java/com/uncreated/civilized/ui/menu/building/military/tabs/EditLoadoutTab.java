package com.uncreated.civilized.ui.menu.building.military.tabs;

import java.util.List;

import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

public class EditLoadoutTab extends ABuildingScreenTab {

   private static final int BUTTON_WIDTH = 60;
   private static final int BUTTON_HEIGHT = 18;

   private final Button cancelButton;
   private final Button saveButton;

   public EditLoadoutTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      super(tabHost, font, Component.translatable("menu.building.military.loadout.edit.tab.heading"), context);

      cancelButton =
            Button.builder(Component.translatable("gui.misc.button.cancel"), button -> backToLoadouts(tabHost))
                  .pos(getX(), getBottom() - BUTTON_HEIGHT)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();

      saveButton =
            Button.builder(Component.translatable("gui.misc.button.save"), button -> backToLoadouts(tabHost))
                  .pos(getRight() - BUTTON_WIDTH, getBottom() - BUTTON_HEIGHT)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();
   }

   private void backToLoadouts(ITabHost tabHost) {
      tabHost.changeTab(new ManageLoadoutsTab(tabHost, font, context));
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);
      graphics.drawWordWrap(
            font,
            Component.translatable("menu.building.military.loadout.edit.placeholder"),
            getX(),
            getY() + 15,
            width,
            Colors.MENU_TEXT_DARK,
            false);
      cancelButton.render(graphics, mouseX, mouseY, partialTicks);
      saveButton.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of(cancelButton, saveButton);
   }
}
