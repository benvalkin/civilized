package com.uncreated.civilized.ui.components.buttons.buildingtab;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ManageResidentsTabButton extends BuildingTabButton {

   private static ResourceLocation ICON =
         ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "icon/building_tab_manage_residents");

   public ManageResidentsTabButton(ITabHost tabHost, int buttonTabIndex) {
      super(
            tabHost,
            buttonTabIndex,
            ICON,
            Tooltip.create(Component.translatable("menu.building.residence.residents.tab.heading")));
   }

   public ManageResidentsTabButton(ITabHost tabHost, int buttonTabIndex, Tooltip tooltip) {
      super(
              tabHost,
              buttonTabIndex,
              ICON,
              tooltip);
   }
}
