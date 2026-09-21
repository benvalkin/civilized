package com.uncreated.civilized.ui.tabs;

import com.uncreated.civilized.ui.components.IRefreshableUI;

import com.uncreated.civilized.ui.menu.building.worksite.animalfarm.tabs.TabCoords;
import lombok.Getter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public abstract class ATab extends AbstractContainerWidget implements IRefreshableUI {
   @Getter
   protected final int index;
   protected final Font font;

   public ATab(int index, TabCoords tabCoords, Font font) {
      super(tabCoords.contentLeftPos(), tabCoords.contentTopPos(), tabCoords.tabWidth(), tabCoords.tabHeight(), Component.literal("Tab placeholder"));
      this.index = index;
      this.font = font;
   }

   protected void init() {

   }

   public boolean showsPlayerInventory() {
      return false;
   }

   protected void onClose() {

   }

   @Override
   protected int contentHeight() {
      return height;
   }

   @Override
   protected double scrollRate() {
      return 1;
   }

   public void tick() {
   }

   public void refresh() {

   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

   }
}
