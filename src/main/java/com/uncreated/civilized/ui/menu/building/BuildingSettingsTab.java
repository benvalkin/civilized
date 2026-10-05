package com.uncreated.civilized.ui.menu.building;

import java.util.List;

import com.uncreated.civilized.client.BuildingRedrawSession;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class BuildingSettingsTab extends ABuildingScreenTab {

   private final Checkbox autoAssignOccupants;
   private final Button redrawBounds;
   private final Button decommission;
   private final Button decommissionConfirm;
   private final Button decommissionCancel;

   private boolean decommissionRequested;

   public BuildingSettingsTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      super(tabHost, font, Component.translatable("menu.building.settings.tab.heading"), context);
      decommissionRequested = false;

      autoAssignOccupants =
            Checkbox.builder(Component.translatable("menu.building.settings.option.auto_assign_occupants"), font)
                  .pos(getX(), getY() + 15)
                  .maxWidth(width)
                  .build();

      boolean canAlterBuilding = AlterBuildingPermissions.hasPermission(context);
      redrawBounds =
            Button.builder(
                  Component.translatable("menu.building.redraw.button"),
                  button -> BuildingRedrawSession.startFromMenu(context.building()))
                  .pos(getX(), getY() + 40)
                  .size(120, 18)
                  .tooltip(
                        Tooltip.create(
                              Component.translatable(
                                    canAlterBuilding ? "menu.building.redraw.button.tooltip"
                                          : "menu.building.redraw.no_permission")))
                  .build();
      redrawBounds.active = canAlterBuilding;

      decommission =
            Button.builder(
                  Component.translatable("menu.building.settings.option.delete_building"),
                  this::onPressDecommission).pos(getX(), getY() + 62).size(120, 18).build();

      decommissionConfirm =
            Button.builder(Component.translatable("gui.misc.button.confirm"), this::onPressDecommissionConfirm)
                  .pos(getX() + width / 2 + 5, getBottom() - 20)
                  .size(width / 2 - 5, 18)
                  .build();

      decommissionCancel =
            Button.builder(Component.translatable("gui.misc.button.cancel"), this::onPressDecommissionCancel)
                  .pos(getX(), getBottom() - 20)
                  .size(width / 2 - 5, 18)
                  .build();

   }

   private void onPressDecommission(Button button) {
      decommissionRequested = true;
   }

   private void onPressDecommissionConfirm(Button button) {
      ClientBuildingStore.INSTANCE.replicateChange(context.building(), StoreOperation.DELETE);
      Minecraft.getInstance().setScreen(null);
      decommissionRequested = false;
   }

   private void onPressDecommissionCancel(Button button) {
      decommissionRequested = false;
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

      if (decommissionRequested) {

         MutableComponent titleDecommission =
               Component.translatable("menu.building.settings.option.delete_building").withStyle(ChatFormatting.RED);
         graphics.drawWordWrap(
               font,
               titleDecommission,
               getX() + (width - font.width(titleDecommission)) / 2,
               getY(),
               width,
               Colors.MENU_TEXT_DARK,
               false);

         graphics.drawWordWrap(
               font,
               Component.translatable("menu.building.settings.option.delete_building.confirm"),
               getX(),
               getY() + 20,
               width,
               Colors.MENU_TEXT_DARK,
               false);
         decommissionConfirm.render(graphics, mouseX, mouseY, partialTicks);
         decommissionCancel.render(graphics, mouseX, mouseY, partialTicks);
         return;
      }

      super.renderWidget(graphics, mouseX, mouseY, partialTicks);
      autoAssignOccupants.render(graphics, mouseX, mouseY, partialTicks);
      redrawBounds.render(graphics, mouseX, mouseY, partialTicks);
      decommission.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      // only what's showing can be clicked, or the hidden confirm button would decommission the building without asking
      if (decommissionRequested)
         return List.of(decommissionConfirm, decommissionCancel);

      return List.of(autoAssignOccupants, redrawBounds, decommission);
   }
}
