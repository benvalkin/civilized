package com.uncreated.civilized.ui.components.widget;

import java.util.List;

import com.uncreated.civilized.core.villagerinfo.skills.SkillValue;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkills;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class SkillWidget extends AbstractWidget {

   private static final int ROW_GAP = 1;
   private static final int LEVEL_BAR_COLOR = 0x88444444;
   private static final int LEVEL_BAR_TRACK_COLOUR = 0x77222222;
   private static final int NEXT_LEVEL_BAR_HEIGHT = 1;
   private static final int NEXT_LEVEL_BAR_COLOUR = 0x44FFFFFF;
   /** Space between the skill's name and the bars. */
   private static final int NAME_GAP = 4;
   /** Space between the bar left pos and the level number text. */
   private static final int LEVEL_INSET = 2;
   private static final String INCAPABLE_LEVEL = "-";

   private final Font font;
   private final SkillValue skill;
   private int barStartX;
   private final boolean showSkillNameLabel;

   public SkillWidget(Font font, int x, int y, int width, SkillValue skill, boolean showSkillNameLabel) {
      super(x, y, width, heightFor(font), skillName(skill));
      this.font = font;
      this.skill = skill;

      this.showSkillNameLabel = showSkillNameLabel;
      if (showSkillNameLabel)
         this.barStartX = getSkillNameRight() + NAME_GAP;
      else
         this.barStartX = x;
   }

   public SkillWidget(Font font, int x, int y, int width, SkillValue skill) {
      this(font, x, y, width, skill, true);
   }

   public int getSkillNameRight() {
      return getX() + font.width(getMessage());
   }

   public void alignBarAfter(int x) {
      this.barStartX = x + NAME_GAP;
   }

   public static int heightFor(Font font) {
      return font.lineHeight + 1 + ROW_GAP;
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

      // drawn first, so the bars stay behind the level
      renderBars(graphics, capable, totalXp, level);

      int textY = getY() + 1;
      if (showSkillNameLabel)
         graphics.drawString(font, getMessage(), getX(), textY, Colors.TEXT_LIGHT_MUTED);
      String levelText = capable ? String.valueOf(level) : INCAPABLE_LEVEL;
      int textColour = capable ? colourFor(level) : Colors.SKILL_UNSKILLED;
      graphics.drawString(font, levelText, barStartX + LEVEL_INSET, textY, textColour);

      if (isHovered())
         graphics.renderComponentTooltip(font, tooltipFor(totalXp, level, capable), mouseX, mouseY);
   }

   private void renderBars(GuiGraphics graphics, boolean capable, int totalXp, int level) {
      int barBottom = getY() + font.lineHeight + 1;
      int barWidth = getRight() - barStartX;
      graphics.fill(barStartX, getY(), getRight(), barBottom, LEVEL_BAR_TRACK_COLOUR);
      if (!capable)
         return;

      int levelWidth = Math.round(barWidth * (float) level / VillagerSkills.MAX_LEVEL);
      graphics.fill(barStartX, getY(), barStartX + levelWidth, barBottom, LEVEL_BAR_COLOR);

      int nextLevelWidth = Math.round(barWidth * progressToNextLevel(totalXp, level));
      graphics.fill(
            barStartX,
            barBottom - NEXT_LEVEL_BAR_HEIGHT,
            barStartX + nextLevelWidth,
            barBottom,
            NEXT_LEVEL_BAR_COLOUR);
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

   static List<Component> tooltipFor(int totalXp, int level, boolean capable) {
      if (!capable)
         return List.of(Component.translatable("villager.skill.level.explanation.incapable"));

      Component progressLine;
      if (level >= VillagerSkills.MAX_LEVEL)
         progressLine = Component.translatable("villager.skill.progress.max");
      else
         progressLine =
               Component.translatable(
                     "villager.skill.progress",
                     String.format("%,d", VillagerSkills.currentLevelXp(totalXp)),
                     String.format("%,d", VillagerSkills.nextLevelRequiredXp(totalXp)));

      return List.of(
            progressLine.copy().withColor(Colors.TEXT_LIGHT_MUTED),
            Component.translatable("villager.skill.level.explanation." + level));
   }

   private static Component skillName(SkillValue skill) {
      return Component.translatable("villager.skill." + skill.getSkill().name().toLowerCase());
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      defaultButtonNarrationText(narrationElementOutput);
   }
}
