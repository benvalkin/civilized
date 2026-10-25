package com.uncreated.civilized.ui.menu.villager.tabs;

import com.uncreated.civilized.ui.tabs.TabCoords;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class FamilyTab extends ComingSoonTab {

   public FamilyTab(TabCoords tabCoords, Font font) {
      super(tabCoords, font, Component.translatable("menu.villager.tab.family"));
   }
}
