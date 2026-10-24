package com.uncreated.civilized.ui.menu.villager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.skills.CapableSkillValue;
import com.uncreated.civilized.core.villagerinfo.skills.SkillValue;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkills;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.data.VillagerHunger;
import com.uncreated.civilized.ui.components.widget.SkillWidget;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class VillagerInfoCardWidget extends AbstractWidget {

   public static final int WIDTH = 140;

   private static final int PADDING = 8;
   private static final int SECTION_GAP = 6;
   private static final int ICON_SIZE = 9;
   private static final int ICON_SPACING = 8;
   private static final int ICON_ROW_GAP = 2;
   private static final int SKILLS_SHOWN = 4;
   private static final SkillWidget.ProgressBarAppearance SKILL_BAR = SkillWidget.ProgressBarAppearance.BEHIND;
   private static final int SKILL_INDENT = 4;

   private static final ResourceLocation HEART_CONTAINER = ResourceLocation.withDefaultNamespace("hud/heart/container");
   private static final ResourceLocation HEART_FULL = ResourceLocation.withDefaultNamespace("hud/heart/full");
   private static final ResourceLocation HEART_HALF = ResourceLocation.withDefaultNamespace("hud/heart/half");
   private static final ResourceLocation FOOD_EMPTY = ResourceLocation.withDefaultNamespace("hud/food_empty");
   private static final ResourceLocation FOOD_FULL = ResourceLocation.withDefaultNamespace("hud/food_full");
   private static final ResourceLocation FOOD_HALF = ResourceLocation.withDefaultNamespace("hud/food_half");

   private final Font font;
   private final CivilizedVillager villager;
   private final List<SkillWidget> skillRows = new ArrayList<>();

   /**
    * @param bottom
    *           the lowest Y coordinate of the card on the screen. The card is as tall as its contents.
    */
   public VillagerInfoCardWidget(Font font, CivilizedVillager villager, int x, int bottom) {
      super(x, 0, WIDTH, heightFor(font), Component.literal(villager.getInfo().getFullName()));
      this.font = font;
      this.villager = villager;
      setY(bottom - height);
      createSkillRows();
   }

   /**
    * Combined pixel height of the following child widgets stacked below each other: Name and title, then health and
    * hunger, then the skills heading and the list of skills.
    */
   private static int heightFor(Font font) {
      return skillListY(font) + SkillWidget.heightFor(font, SKILL_BAR) * SKILLS_SHOWN + PADDING;
   }

   private static int skillListY(Font font) {
      return PADDING + font.lineHeight * 2 + SECTION_GAP + ICON_SIZE * 2 + ICON_ROW_GAP + SECTION_GAP + font.lineHeight;
   }

   private void createSkillRows() {
      int x = getX() + PADDING + SKILL_INDENT;
      int y = getY() + skillListY(font);
      int rowWidth = getRight() - PADDING - x;
      for (CapableSkillValue skill : findBestSkills(villager.getInfo().getSkills())) {
         skillRows.add(
               new SkillWidget(
                     font,
                     x,
                     y,
                     rowWidth,
                     new SkillValue(skill.skill(), skill.totalXp()),
                     SkillWidget.ProgressBarCalculation.LEVEL_OUT_OF_MAX,
                     SKILL_BAR));
         y += SkillWidget.heightFor(font, SKILL_BAR);
      }

      // every row's bar starts after the longest name, so they line up
      int longestNameRight = x;
      for (SkillWidget row : skillRows)
         longestNameRight = Math.max(longestNameRight, row.getNameRight());
      for (SkillWidget row : skillRows)
         row.alignBarAfter(longestNameRight);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      // card does not respond to clicks
      return false;
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(getX(), getY(), getRight(), getBottom(), VillagerScreen.PANEL_COLOUR);

      VillagerInfo info = villager.getInfo();
      int x = getX() + PADDING;
      int y = getY() + PADDING;
      int centreX = getX() + width / 2;

      graphics.drawCenteredString(font, info.getFirstName(), centreX, y, Colors.MENU_TEXT_VILLAGER_DIALOGUE);
      y += font.lineHeight;
      graphics.drawCenteredString(
            font,
            info.getTitleTranslation().copy().withColor(Colors.BUILDING_LIGHT),
            centreX,
            y,
            Colors.TEXT_LIGHT_MUTED);
      y += font.lineHeight + SECTION_GAP;

      renderHealth(graphics, x, y);
      y += ICON_SIZE + ICON_ROW_GAP;
      renderHunger(graphics, x, y);
      y += ICON_SIZE + SECTION_GAP;

      graphics
            .drawString(font, Component.translatable("menu.villager.skills"), x, y, Colors.MENU_TEXT_VILLAGER_DIALOGUE);
      skillRows.forEach(row -> row.render(graphics, mouseX, mouseY, partialTick));
   }

   private void renderHealth(GuiGraphics graphics, int x, int y) {
      int hearts = Math.min(10, Mth.ceil(villager.getMaxHealth() / 2));
      int health = Mth.ceil(villager.getHealth());
      for (int i = 0; i < hearts; i++) {
         int iconX = x + i * ICON_SPACING;
         graphics.blitSprite(RenderType::guiTextured, HEART_CONTAINER, iconX, y, ICON_SIZE, ICON_SIZE);
         if (health >= i * 2 + 2)
            graphics.blitSprite(RenderType::guiTextured, HEART_FULL, iconX, y, ICON_SIZE, ICON_SIZE);
         else if (health == i * 2 + 1)
            graphics.blitSprite(RenderType::guiTextured, HEART_HALF, iconX, y, ICON_SIZE, ICON_SIZE);
      }
   }

   private void renderHunger(GuiGraphics graphics, int x, int y) {
      int hunger = villager.getHunger().hunger();
      int icons = VillagerHunger.MAX_HUNGER / 2;
      for (int i = 0; i < icons; i++) {
         // hunger icons are drawn from right to left (like the player's hunger bar) otherwise the half-hunger icon
         // looks like it's flipped.
         int iconX = x + (icons - 1 - i) * ICON_SPACING;
         graphics.blitSprite(RenderType::guiTextured, FOOD_EMPTY, iconX, y, ICON_SIZE, ICON_SIZE);
         if (hunger >= i * 2 + 2)
            graphics.blitSprite(RenderType::guiTextured, FOOD_FULL, iconX, y, ICON_SIZE, ICON_SIZE);
         else if (hunger == i * 2 + 1)
            graphics.blitSprite(RenderType::guiTextured, FOOD_HALF, iconX, y, ICON_SIZE, ICON_SIZE);
      }
   }

   private static List<CapableSkillValue> findBestSkills(VillagerSkills skills) {
      List<CapableSkillValue> best = skills.capableSkills();
      best.sort(Comparator.comparingInt(CapableSkillValue::totalXp).reversed());
      return best.subList(0, Math.min(SKILLS_SHOWN, best.size()));
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      defaultButtonNarrationText(narrationElementOutput);
   }
}
