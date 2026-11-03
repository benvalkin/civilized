package com.uncreated.civilized.ui.components.widget;

import java.util.ArrayList;
import java.util.List;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.core.villagerinfo.skills.SkillValue;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkills;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SkillIconWidget extends AbstractWidget {

   private static final int ICON_SIZE = 8;
   private static final int ICON_GAP = 2;
   private static final String INCAPABLE_LEVEL = "-";

   private final Font font;
   private final SkillValue skill;

   public SkillIconWidget(Font font, int x, int y, SkillValue skill) {
      super(x, y, widthFor(font), font.lineHeight, skillName(skill));
      this.font = font;
      this.skill = skill;
   }

   public static int widthFor(Font font) {
      return ICON_SIZE + ICON_GAP + font.width(String.valueOf(VillagerSkills.MAX_LEVEL));
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

      graphics.blitSprite(RenderType::guiTextured, iconFor(skill), getX(), getY(), ICON_SIZE, ICON_SIZE, 2);

      String levelText = capable ? String.valueOf(level) : INCAPABLE_LEVEL;
      int colour = capable ? Colors.MENU_TEXT_DARK : Colors.TEXT_DARK_MUTED;
      graphics.drawString(font, levelText, getX() + ICON_SIZE + ICON_GAP, getY(), colour, false);

      if (isHovered())
         graphics.renderComponentTooltip(font, tooltipFor(totalXp, level, capable), mouseX, mouseY);
   }

   private List<Component> tooltipFor(int totalXp, int level, boolean capable) {
      List<Component> lines = new ArrayList<>();
      lines.add(getMessage());
      lines.addAll(SkillWidget.tooltipFor(totalXp, level, capable));
      return lines;
   }

   private static ResourceLocation iconFor(SkillValue skill) {
      return ResourceLocation
            .fromNamespaceAndPath(CivilizedMod.CIVILIZED_MOD_ID, "icon/skill_" + skill.getSkill().name().toLowerCase());
   }

   private static Component skillName(SkillValue skill) {
      return Component.translatable("villager.skill." + skill.getSkill().name().toLowerCase());
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      defaultButtonNarrationText(narrationElementOutput);
   }
}
