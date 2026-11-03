package com.uncreated.civilized.ui.menu.building.occupants;

import java.util.List;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkill;
import com.uncreated.civilized.ui.components.widget.SkillIconWidget;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class OccupantRowWidget extends AbstractContainerWidget {

   public static final int HEIGHT = 18;
   public static final int BUTTON_WIDTH = 50;
   public static final int BUTTON_HEIGHT = 14;

   private static final int DETAIL_X = 110;
   private static final int DETAIL_GAP = 4;

   private final Font font;
   private final VillagerInfo villager;
   @Nullable
   private final Button button;
   @Nullable
   private final SkillIconWidget skill;

   public OccupantRowWidget(int x, int y, int width, Font font, VillagerInfo villager, @Nullable Button button) {
      this(x, y, width, font, villager, button, null);
   }

   public OccupantRowWidget(
         int x,
         int y,
         int width,
         Font font,
         VillagerInfo villager,
         @Nullable Button button,
         @Nullable VillagerSkill skill) {
      super(x, y, width, HEIGHT, Component.literal(villager.getFullName()));
      this.font = font;
      this.villager = villager;
      this.button = button;
      if (button != null) { // button can be repositioned anywhere
         button.setPosition(x + width - BUTTON_WIDTH, y + (HEIGHT - BUTTON_HEIGHT) / 2);
         button.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
      }

      if (skill != null) {
         int skillX = getButtonLeft() - DETAIL_GAP - SkillIconWidget.widthFor(font);
         int skillY = y + (HEIGHT - font.lineHeight) / 2 + 1;
         this.skill = new SkillIconWidget(font, skillX, skillY, villager.getSkills().getSkill(skill));
      } else
         this.skill = null;
   }

   private int getButtonLeft() {
      return button != null ? button.getX() : getRight() + DETAIL_GAP;
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      int textY = getY() + (HEIGHT - font.lineHeight) / 2 + 1;
      graphics.drawString(font, villager.getFullName(), getX(), textY, Colors.MENU_TEXT_DARK, false);

      int detailRight = (skill != null ? skill.getX() : getButtonLeft()) - DETAIL_GAP;
      String job =
            font.plainSubstrByWidth(villager.getTitleTranslation().getString(), detailRight - (getX() + DETAIL_X));
      graphics.drawString(font, job, getX() + DETAIL_X, textY, Colors.BUILDING_DARK, false);

      if (skill != null)
         skill.render(graphics, mouseX, mouseY, partialTick);

      if (button != null)
         button.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return button != null ? List.of(button) : List.of();
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
