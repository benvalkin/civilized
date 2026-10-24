package com.uncreated.civilized.ui.components.widget;

import java.util.Random;

import com.uncreated.civilized.core.villagerinfo.skills.SkillValue;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkills;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class SkillWidget extends AbstractWidget {

   private static final int BAR_GAP = 1;
   private static final int BAR_HEIGHT = 2;
   private static final int ROW_GAP = 1;
   private static final int BAR_ALPHA = 0xA0;
   private static final int BAR_TRACK_COLOUR = 0x30FFFFFF;
   private static final String INCAPABLE_LEVEL = "-";

   private final Font font;
   private final SkillValue skill;

   public SkillWidget(Font font, int x, int y, int width, SkillValue skill) {
      super(x, y, width, heightFor(font), skillName(skill));
      this.font = font;
      this.skill = skill;
   }

   /**
    * Combined pixel height of the following child widgets stacked below each other: The skill's line of text, its
    * progress bar, and a gap before the next one.
    */
   public static int heightFor(Font font) {
      return font.lineHeight + BAR_GAP + BAR_HEIGHT + ROW_GAP;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      // read only widget - mouse clicks are ignored
      return false;
   }

   // todo: this is for debug - remove later
   int random = new Random().nextInt(VillagerSkills.MAX_TOTAL_XP);

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      boolean capable = skill.totalXp().isPresent();
      // int totalXp = skill.totalXp().orElse(0);
      int totalXp = random;
      int level = VillagerSkills.currentLevel(totalXp);
      int colour = capable ? colourFor(level) : Colors.SKILL_UNSKILLED;

      graphics.drawString(font, getMessage(), getX(), getY(), Colors.TEXT_LIGHT_MUTED);
      String levelText = capable ? String.valueOf(level) : INCAPABLE_LEVEL;
      graphics.drawString(font, levelText, getRight() - font.width(levelText), getY(), colour);

      int barY = getY() + font.lineHeight + BAR_GAP;
      graphics.fill(getX(), barY, getRight(), barY + BAR_HEIGHT, BAR_TRACK_COLOUR);
      if (capable) {
         int filledWidth = Math.round(width * progressToNextLevel(totalXp, level));
         graphics.fill(getX(), barY, getX() + filledWidth, barY + BAR_HEIGHT, BAR_ALPHA << 24 | colour);
      }

      if (isHovered())
         graphics.renderTooltip(font, levelName(capable, level), mouseX, mouseY);
   }

   private static float progressToNextLevel(int totalXp, int level) {
      if (level >= VillagerSkills.MAX_LEVEL)
         return 1F;

      return (float) VillagerSkills.currentLevelXp(totalXp) / VillagerSkills.nextLevelRequiredXp(totalXp);
   }

   public static int colourFor(int level) {
      if (level <= 2)
         return Colors.SKILL_UNSKILLED;
      if (level <= 6)
         return Colors.SKILL_COMPETENT;
      if (level <= 8)
         return Colors.SKILL_HIGHLY_SKILLED;
      return Colors.SKILL_MASTER;
   }

   private static Component levelName(boolean capable, int level) {
      if (!capable)
         return Component.translatable("villager.skill.level.explanation.incapable");
      return Component.translatable("villager.skill.level.explanation." + level);
   }

   private static Component skillName(SkillValue skill) {
      return Component.translatable("villager.skill." + skill.getSkill().name().toLowerCase());
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      defaultButtonNarrationText(narrationElementOutput);
   }
}
