package com.uncreated.civilized.ui.menu.building.military.tabs;

import java.util.List;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;
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
   /** The loadout being edited, or null when creating a new one. */
   @Nullable
   private final Loadout loadout;

   /** Creates a new loadout. */
   public EditLoadoutTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      this(tabHost, font, context, null);
   }

   public EditLoadoutTab(ITabHost tabHost, Font font, BuildingScreenContext context, @Nullable Loadout loadout) {
      super(tabHost, font, Component.translatable("menu.building.military.loadout.edit.tab.heading"), context);
      this.loadout = loadout;

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
