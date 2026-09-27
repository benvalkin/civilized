package com.uncreated.civilized.ui.menu.building.widgets;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.settlement.permission.AccessLevel;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ManagePermissionWidget extends AbstractContainerWidget {

   private static final int BUTTON_WIDTH = 80;
   private static final int BUTTON_HEIGHT = 14;

   private final Font font;
   private final String playerName;
   private final Button button;

   public ManagePermissionWidget(
         int x,
         int y,
         int width,
         int height,
         Font font,
         String playerName,
         @Nullable AccessLevel accessLevel,
         boolean canManage,
         boolean isViewer,
         Consumer<AccessLevel> onChangeRequested) {
      super(x, y, width, height, Component.literal("ManagePermissionWidget"));
      this.font = font;
      this.playerName = playerName;

      @Nullable
      AccessLevel nextAccessLevel = next(accessLevel);
      boolean canChange = canManage && !isViewer;

      button =
            Button.builder(translation(accessLevel), b -> onChangeRequested.accept(nextAccessLevel))
                  .pos(x + width - BUTTON_WIDTH, y + (height - BUTTON_HEIGHT) / 2)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();
      button.active = canChange;
      button.setTooltip(Tooltip.create(createTooltip(accessLevel, nextAccessLevel, canChange, isViewer)));
   }

   private static Component createTooltip(
         @Nullable AccessLevel accessLevel,
         @Nullable AccessLevel nextAccessLevel,
         boolean canChange,
         boolean isViewer) {
      MutableComponent tooltip =
            (accessLevel == null ? AccessLevel.noAccessDescription() : accessLevel.description()).copy();

      if (isViewer)
         tooltip.append("\n\n")
               .append(
                     Component.translatable("menu.building.town_hall.permissions.own_access_level")
                           .withStyle(ChatFormatting.GRAY));
      else if (canChange)
         tooltip.append("\n\n")
               .append(
                     Component
                           .translatable("menu.building.town_hall.permissions.click_to_change", translation(nextAccessLevel))
                           .withStyle(ChatFormatting.GRAY));

      return tooltip;
   }

   private static Component translation(@Nullable AccessLevel accessLevel) {
      return accessLevel == null ? AccessLevel.noAccessTranslation() : accessLevel.translation();
   }

   private static @Nullable AccessLevel next(@Nullable AccessLevel accessLevel) {
      AccessLevel[] levels = AccessLevel.values();
      if (accessLevel == null)
         return levels[0];
      if (accessLevel.ordinal() == levels.length - 1)
         return null;
      return levels[accessLevel.ordinal() + 1];
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
   protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      guiGraphics.drawString(
            font,
            playerName,
            getX(),
            getY() + (height - font.lineHeight) / 2,
            Colors.MENU_TEXT_DARK,
            false);
      button.render(guiGraphics, mouseX, mouseY, partialTick);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of(button);
   }
}
