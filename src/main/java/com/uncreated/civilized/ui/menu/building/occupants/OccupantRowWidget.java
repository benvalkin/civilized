package com.uncreated.civilized.ui.menu.building.occupants;

import java.util.List;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
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

   private final Font font;
   private final VillagerInfo villager;
   @Nullable
   private final Button button;

   public OccupantRowWidget(int x, int y, int width, Font font, VillagerInfo villager, @Nullable Button button) {
      super(x, y, width, HEIGHT, Component.literal(villager.getFullName()));
      this.font = font;
      this.villager = villager;
      this.button = button;
      if (button != null) { // button can be repositioned anywhere
         button.setPosition(x + width - BUTTON_WIDTH, y + (HEIGHT - BUTTON_HEIGHT) / 2);
         button.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
      }
   }

   public static Component describeJob(VillagerInfo villager) {
      if (!villager.getOccupation().is(VillagerOccupations.UNEMPLOYED))
         return villager.getOccupation().translation();
      if (villager.getNpcRole().is(VillagerNpcRoles.SPOUSE))
         return Component.translatable("menu.building.occupants.job.spouse"); // todo: remove this when spouses are folded
      return Component.translatable("menu.building.occupants.job.unemployed");
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      int textY = getY() + (HEIGHT - font.lineHeight) / 2 + 1;
      graphics.drawString(font, villager.getFullName(), getX(), textY, Colors.MENU_TEXT_DARK, false);

      int detailRight = button != null ? button.getX() - 4 : getRight();
      String job = font.plainSubstrByWidth(describeJob(villager).getString(), detailRight - (getX() + DETAIL_X));
      graphics.drawString(font, job, getX() + DETAIL_X, textY, Colors.BUILDING_DARK, false);

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
