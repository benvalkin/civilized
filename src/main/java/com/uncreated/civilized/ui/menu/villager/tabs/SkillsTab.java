package com.uncreated.civilized.ui.menu.villager.tabs;

import java.util.ArrayList;
import java.util.List;

import com.uncreated.civilized.core.villagerinfo.skills.SkillValue;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.ui.components.widget.SkillWidget;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.TabCoords;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

public class SkillsTab extends AVillagerPanelTab {

   private static final int HEADING_GAP = 6;
   private static final SkillWidget.ProgressBarAppearance SKILL_BAR = SkillWidget.ProgressBarAppearance.BEHIND;

   private final CivilizedVillager villager;
   private final List<SkillWidget> skillRows = new ArrayList<>();

   public SkillsTab(TabCoords tabCoords, Font font, CivilizedVillager villager) {
      super(tabCoords, font);
      this.villager = villager;
   }

   @Override
   protected void init() {
      int x = getX() + PADDING;
      int y = getY() + PADDING + font.lineHeight + HEADING_GAP;
      int rowWidth = width - PADDING * 2;
      for (SkillValue skill : villager.getInfo().getSkills().allSkills()) {
         SkillWidget row =
               new SkillWidget(
                     font,
                     x,
                     y,
                     rowWidth,
                     skill,
                     SkillWidget.ProgressBarCalculation.TOWARDS_NEXT_LEVEL,
                     SKILL_BAR);
         skillRows.add(row);
         y += SkillWidget.heightFor(font, SKILL_BAR);
      }

      // every row's bar starts after the longest name so that they line up nicely
      int longestNameRight = x;
      for (SkillWidget row : skillRows)
         longestNameRight = Math.max(longestNameRight, row.getNameRight());
      for (SkillWidget row : skillRows)
         row.alignBarAfter(longestNameRight);
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.drawString(
            font,
            Component.translatable("menu.villager.tab.skills"),
            getX() + PADDING,
            getY() + PADDING,
            Colors.MENU_TEXT_VILLAGER_DIALOGUE);

      for (SkillWidget row : skillRows)
         row.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return skillRows;
   }
}
