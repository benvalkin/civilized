package com.uncreated.civilized.ui.menu.building.worksite.grove.tabs;

import java.util.List;

import com.uncreated.civilized.core.building.state.GroveState;
import com.uncreated.civilized.networking.packets.SetEyeDropperSlotItem;
import com.uncreated.civilized.ui.components.widget.EyeDropperSlotWidget;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class SaplingTab extends ABuildingScreenTab {

   private static final int DESCRIPTION_Y = 12;
   private static final int SLOTS_Y = 38;

   private final EyeDropperSlotWidget saplingSlot;

   public SaplingTab(ITabHost tabHost, Font font, BuildingScreenContext context, AbstractContainerScreen<?> screen) {
      super(tabHost, font, Component.translatable("menu.building.worksite.grove.allowed_saplings.heading"), context);

      saplingSlot =
            new EyeDropperSlotWidget(
                  screen,
                  getX() + (width - EyeDropperSlotWidget.SIZE) / 2,
                  getY() + SLOTS_Y,
                  () -> groveState().getSapling(),
                  stack -> groveState().isValidEyeDropperSlotItem(0, stack),
                  stack -> PacketDistributor
                        .sendToServer(new SetEyeDropperSlotItem(context.building().getBuildingId(), 0, stack)));
   }

   private GroveState groveState() {
      return (GroveState) context.building().getState();
   }

   @Override
   public boolean showsPlayerInventory() {
      return true;
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);

      graphics.drawWordWrap(
            font,
            Component.translatable("menu.building.worksite.grove.allowed_saplings.description"),
            getX(),
            getY() + DESCRIPTION_Y,
            width,
            Colors.MENU_TEXT_DARK,
            false);

      saplingSlot.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of(saplingSlot);
   }
}
