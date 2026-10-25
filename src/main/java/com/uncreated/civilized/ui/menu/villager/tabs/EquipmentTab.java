package com.uncreated.civilized.ui.menu.villager.tabs;

import com.uncreated.civilized.ui.tabs.TabCoords;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class EquipmentTab extends ComingSoonTab {

   public EquipmentTab(TabCoords tabCoords, Font font) {
      super(tabCoords, font, Component.translatable("menu.villager.tab.equipment"));
   }

   @Override
   public boolean showsPlayerInventory() {
      return true;
   }
}
