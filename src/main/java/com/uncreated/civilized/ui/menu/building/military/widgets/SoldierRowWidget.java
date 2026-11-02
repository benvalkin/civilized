package com.uncreated.civilized.ui.menu.building.military.widgets;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.workshift.WorkShift;
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
   private static final int TEXT_GAP = 4;
   private static final int LINE_SPACING = 1;
   private static final String DETAIL_SEPARATOR = " · ";
   private static final String ELLIPSIS = "...";

   private final Font font;
   private final VillagerInfo soldier;
   @Nullable
   private final String loadoutName;
   private final WorkShift defaultWorkShift;
   private final Button manageButton;

   public SoldierRowWidget(
         int x,
         int y,
         int width,
         int height,
         Font font,
         VillagerInfo soldier,
         @Nullable String loadoutName,
         Consumer<VillagerInfo> onManage) {
      super(x, y, width, height, Component.literal(soldier.getFullName()));
      this.font = font;
      this.soldier = soldier;
      this.loadoutName = loadoutName;

      manageButton =
            Button.builder(
                  Component.translatable("menu.building.military.soldiers.button.manage"),
                  button -> onManage.accept(soldier))
                  .pos(x + width - BUTTON_WIDTH, y + (height - BUTTON_HEIGHT) / 2)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();

      defaultWorkShift =
            ClientBuildingStore.INSTANCE.find(soldier.getWorkBuildingId())
                  .map(post -> post.getBuildingType().defaultWorkshift())
                  .orElse(WorkShift.UNEMPLOYED);
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      int textRight = manageButton.getX() - TEXT_GAP;
      int nameY = getY() + (height - font.lineHeight * 2 - LINE_SPACING) / 2 + 1;
      graphics.drawString(
            font,
            fitToWidth(soldier.getFullName(), textRight - getX()),
            getX(),
            nameY,
            Colors.MENU_TEXT_DARK,
            false);

      renderDetails(graphics, nameY + font.lineHeight + LINE_SPACING, textRight);
      manageButton.render(graphics, mouseX, mouseY, partialTick);
   }

   private void renderDetails(GuiGraphics graphics, int y, int textRight) {
      WorkShift assigned = soldier.getWorkshift();
      String workShift = (assigned != null ? assigned : defaultWorkShift).translation().getString();
      // the post's default is greyed out, so shifts picked for this soldier stand out
      int workShiftColor = assigned != null ? Colors.TEXT_DARK_MUTED : Colors.TEXT_DARK_EXTRA_MUTED;

      if (loadoutName != null)
         graphics.drawString(font, fitToWidth(loadoutName, 70), getX() + 8, y, Colors.BUILDING_DARK, false);

      graphics.drawString(font, fitToWidth(workShift, 70), getX() + 78, y, workShiftColor, false);

   }

   private String fitToWidth(String text, int maxWidth) {
      if (font.width(text) <= maxWidth)
         return text;

      return font.plainSubstrByWidth(text, maxWidth - font.width(ELLIPSIS)) + ELLIPSIS;
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
