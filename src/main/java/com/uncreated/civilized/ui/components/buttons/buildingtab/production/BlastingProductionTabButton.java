package com.uncreated.civilized.ui.components.buttons.buildingtab.production;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.function.Function;

import com.uncreated.civilized.core.building.production.bills.ProductionTypes;
import com.uncreated.civilized.ui.components.buttons.buildingtab.BuildingTabButton;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.resources.ResourceLocation;

public class BlastingProductionTabButton extends BuildingTabButton {

   private static ResourceLocation ICON =
         ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "icon/building_tab_production_blasting");

   public BlastingProductionTabButton(ITabHost tabHost, int buttonTabIndex, Function<ITabHost, ATab> createTab) {
      super(tabHost, buttonTabIndex, ICON, Tooltip.create(ProductionTypes.BLASTING.heading()), createTab);
   }
}
