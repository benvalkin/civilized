package com.uncreated.civilized.ui.menu.building.widgets;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.settlement.permission.AccessLevel;
import com.uncreated.civilized.networking.packets.SetSettlementAccessLevel;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ManagePermissionWidget extends AbstractContainerWidget {

   private static final int BUTTON_WIDTH = 80;
   private static final int BUTTON_HEIGHT = 14;

   private final Font font;
   private final UUID settlementId;
   private final UUID playerId;
   private final String playerName;
   private final @Nullable AccessLevel accessLevel;
   private final Button button;

   public ManagePermissionWidget(
         int x,
         int y,
         int width,
         int height,
         Font font,
         UUID settlementId,
         UUID playerId,
         String playerName,
         @Nullable AccessLevel accessLevel,
         boolean canManage) {
      super(x, y, width, height, Component.literal("ManagePermissionWidget"));
      this.font = font;
      this.settlementId = settlementId;
      this.playerId = playerId;
      this.playerName = playerName;
      this.accessLevel = accessLevel;

      Component buttonText = accessLevel == null ? AccessLevel.noAccessTranslation() : accessLevel.translation();
      button =
            Button.builder(buttonText, this::onPress)
                  .pos(x + width - BUTTON_WIDTH, y + (height - BUTTON_HEIGHT) / 2)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();
      button.active = canManage;
   }

   /** Goes up one access level at a time, and from the highest back round to no access. */
   private static @Nullable AccessLevel next(@Nullable AccessLevel accessLevel) {
      AccessLevel[] levels = AccessLevel.values();
      if (accessLevel == null)
         return levels[0];
      if (accessLevel.ordinal() == levels.length - 1)
         return null;
      return levels[accessLevel.ordinal() + 1];
   }

   private void onPress(Button b) {
      PacketDistributor.sendToServer(
            new SetSettlementAccessLevel(settlementId, playerId, Optional.ofNullable(next(accessLevel))));
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
