package com.uncreated.civilized.ui.menu.building.military.widgets;

import java.util.List;
import java.util.function.Consumer;

import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class SoldierRowWidget extends AbstractContainerWidget {

   private static final int BUTTON_WIDTH = 50;
   private static final int BUTTON_HEIGHT = 14;

   private final Font font;
   private final VillagerInfo soldier;
   private final Button manageButton;

   public SoldierRowWidget(
         int x,
         int y,
         int width,
         int height,
         Font font,
         VillagerInfo soldier,
         Consumer<VillagerInfo> onManage) {
      super(x, y, width, height, Component.literal(soldier.getFullName()));
      this.font = font;
      this.soldier = soldier;

      manageButton =
            Button.builder(
                  Component.translatable("menu.building.military.soldiers.button.manage"),
                  button -> onManage.accept(soldier))
                  .pos(x + width - BUTTON_WIDTH, y + (height - BUTTON_HEIGHT) / 2)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.drawString(
            font,
            soldier.getFullName(),
            getX(),
            getY() + (height - font.lineHeight) / 2,
            Colors.MENU_TEXT_DARK,
            false);
      manageButton.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of(manageButton);
   }

   @Override
   protected int contentHeight() {
      return height;
   }

   @Override
   protected double scrollRate() {
      return 0;
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
   }
}
