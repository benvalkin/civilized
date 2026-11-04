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

public class CompactSkillWidget extends AbstractWidget {

   private static final int ROW_GAP = 1;
   private static final int BAR_HEIGHT = 2;
   private static final int LEVEL_BAR_COLOR = 0xffb58e79;

   private final Font font;
   private final SkillValue skill;

   public CompactSkillWidget(Font font, int x, int y, int width, SkillValue skill) {
      super(x, y, width, heightFor(font), skillName(skill));
      this.font = font;
      this.skill = skill;
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

      renderBars(graphics, level);
      graphics.drawString(font, levelText(level, capable), getX(), getY(), levelTextColor(level), false);

      if (isHovered())
         graphics.renderComponentTooltip(font, tooltipFor(totalXp, level, capable), mouseX, mouseY);
   }

   private String levelText(int level, boolean capable) {
      if (!capable)
         return "-";

      return String.valueOf(level);
   }

   private int levelTextColor(int level) {
      if (level <= 0)
         return Colors.TEXT_DARK_EXTRA_MUTED;

      return Colors.TEXT_DARK_MUTED;
   }

   private void renderBars(GuiGraphics graphics, int level) {
      int barStartX = getX();
      int barWidth = getRight() - barStartX;
      // digits only use the top 7 rows of a line, so the bar can sit just under them
      int barStartY = getY() + font.lineHeight - 1;
      int barBottom = barStartY + BAR_HEIGHT;
      int levelWidth = Math.round(barWidth * (float) level / VillagerSkills.MAX_LEVEL);

      graphics.fill(barStartX, barStartY, barStartX + levelWidth, barBottom, LEVEL_BAR_COLOR);
   }

   private List<Component> tooltipFor(int totalXp, int level, boolean capable) {
      if (!capable)
         return List.of(Component.translatable("villager.skill.level.explanation.incapable.of"), skillName(skill));

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
            skillName(skill),
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
