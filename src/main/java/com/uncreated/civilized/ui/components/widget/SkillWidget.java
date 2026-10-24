package com.uncreated.civilized.ui.components.widget;

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
   private static final int BAR_TRACK_COLOUR = 0x20FFFFFF;
   /** A bar behind the level is much bigger, so it's fainter to keep the text readable. */
   private static final int BEHIND_BAR_ALPHA = 0x30;
   private static final int BEHIND_BAR_TRACK_COLOUR = 0x10FFFFFF;
   /** Space between the skill's name and a bar behind the level. */
   private static final int NAME_GAP = 4;
   /** Keeps the level number off the edge of a bar behind it. */
   private static final int LEVEL_INSET = 2;
   private static final String INCAPABLE_LEVEL = "-";

   private final Font font;
   private final SkillValue skill;

   public enum ProgressBarCalculation {
      /** Current level / highest possible level. */
      LEVEL_OUT_OF_MAX,
      /** Villager's current level XP / XP required to reach their next level. */
      TOWARDS_NEXT_LEVEL
   }

   public enum ProgressBarAppearance {
      /** A thin, highly visible bar under the skill's name and level. */
      UNDERNEATH,
      /** A faint bar filling the row to the right of the skill's name, behind the level. */
      BEHIND
   }

   private final ProgressBarCalculation progress;
   private final ProgressBarAppearance bar;
   private int barStartX;

   public SkillWidget(
         Font font,
         int x,
         int y,
         int width,
         SkillValue skill,
         ProgressBarCalculation progress,
         ProgressBarAppearance bar) {
      super(x, y, width, heightFor(font, bar), skillName(skill));
      this.font = font;
      this.skill = skill;
      this.progress = progress;
      this.bar = bar;
      this.barStartX = getNameRight() + NAME_GAP;
   }

   /** Where the skill's name ends. */
   public int getNameRight() {
      return getX() + font.width(getMessage());
   }

   public void alignBarAfter(int x) {
      this.barStartX = x + NAME_GAP;
   }

   /**
    * Combined pixel height of the following child widgets stacked below each other: The skill's line of text, its
    * progress bar, and a gap before the next one.
    */
   public static int heightFor(Font font, ProgressBarAppearance bar) {
      return switch (bar) {
      case UNDERNEATH -> font.lineHeight + BAR_GAP + BAR_HEIGHT + ROW_GAP;
      // the text sits a pixel down inside the bar
      case BEHIND -> font.lineHeight + 1 + ROW_GAP;
      };
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      // read only widget - mouse clicks are ignored
      return false;
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      boolean capable = skill.totalXp().isPresent();
      int totalXp = skill.totalXp().orElse(0);
      int level = VillagerSkills.currentLevel(totalXp);
      int colour = capable ? colourFor(level) : Colors.SKILL_UNSKILLED;

      float filled = 0;
      if (capable)
         filled = switch (progress) {
         case LEVEL_OUT_OF_MAX -> (float) level / VillagerSkills.MAX_LEVEL;
         case TOWARDS_NEXT_LEVEL -> progressToNextLevel(totalXp, level);
         };

      // drawn first, so a bar behind the level stays behind the text
      int textY = renderBar(graphics, filled, colour);

      graphics.drawString(font, getMessage(), getX(), textY, Colors.TEXT_LIGHT_MUTED);
      String levelText = capable ? String.valueOf(level) : INCAPABLE_LEVEL;
      int levelRight = bar == ProgressBarAppearance.BEHIND ? getRight() - LEVEL_INSET : getRight();
      graphics.drawString(font, levelText, levelRight - font.width(levelText), textY, colour);

      if (isHovered())
         graphics.renderTooltip(font, levelName(capable, level), mouseX, mouseY);
   }

   private int renderBar(GuiGraphics graphics, float filled, int colour) {
      switch (bar) {
      case UNDERNEATH -> {
         int barY = getY() + font.lineHeight + BAR_GAP;
         graphics.fill(getX(), barY, getRight(), barY + BAR_HEIGHT, BAR_TRACK_COLOUR);
         int filledWidth = Math.round(width * filled);
         graphics.fill(getX(), barY, getX() + filledWidth, barY + BAR_HEIGHT, BAR_ALPHA << 24 | colour);
         return getY();
      }
      case BEHIND -> {
         int barBottom = getY() + font.lineHeight + 1;
         graphics.fill(barStartX, getY(), getRight(), barBottom, BEHIND_BAR_TRACK_COLOUR);
         int filledWidth = Math.round((getRight() - barStartX) * filled);
         graphics.fill(barStartX, getY(), barStartX + filledWidth, barBottom, BEHIND_BAR_ALPHA << 24 | colour);
         return getY() + 1;
      }
      }
      throw new IllegalStateException("Unknown bar: " + bar);
   }

   private static float progressToNextLevel(int totalXp, int level) {
      if (level >= VillagerSkills.MAX_LEVEL)
         return 1F;

      return (float) VillagerSkills.currentLevelXp(totalXp) / VillagerSkills.nextLevelRequiredXp(totalXp);
   }

   public static int colourFor(int level) {
      if (level <= 0)
         return Colors.SKILL_INCAPABLE;
      if (level <= 3)
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
