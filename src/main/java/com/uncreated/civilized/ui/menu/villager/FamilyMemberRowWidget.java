package com.uncreated.civilized.ui.menu.villager;

import com.uncreated.civilized.ui.menu.villager.tabs.FamilyMember;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class FamilyMemberRowWidget extends AbstractWidget {

   public static final int HEIGHT = 12;

   private static final int NAME_X = 64;
   private static final int TITLE_GAP = 6;

   private final Font font;
   private final FamilyMember member;

   public FamilyMemberRowWidget(int x, int y, int width, Font font, FamilyMember member) {
      super(x, y, width, HEIGHT, Component.literal(member.info().getFullName()));
      this.font = font;
      this.member = member;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      // read only widget - mouse clicks are ignored
      return false;
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      int textY = getY() + (HEIGHT - font.lineHeight) / 2 + 1;
      graphics.drawString(font, member.relationTranslation(), getX(), textY, Colors.TEXT_LIGHT_MUTED);

      Component title = member.info().getTitleTranslation();
      int titleX = getRight() - font.width(title);
      graphics.drawString(font, title, titleX, textY, Colors.BUILDING_LIGHT);

      // long names are cut short
      int nameX = getX() + NAME_X;
      String name = font.plainSubstrByWidth(member.info().getFullName(), titleX - TITLE_GAP - nameX);
      graphics.drawString(font, name, nameX, textY, Colors.MENU_TEXT_VILLAGER_DIALOGUE);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      defaultButtonNarrationText(narrationElementOutput);
   }
}
