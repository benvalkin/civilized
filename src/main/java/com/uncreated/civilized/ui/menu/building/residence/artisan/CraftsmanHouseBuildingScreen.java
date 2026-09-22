package com.uncreated.civilized.ui.menu.building.residence.artisan;

import java.util.List;

import com.uncreated.civilized.core.building.production.bills.ProductionTypes;
import com.uncreated.civilized.ui.components.buttons.buildingtab.HomeTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.ManageResidentsTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.SettingsTabButton;
import com.uncreated.civilized.ui.components.buttons.buildingtab.production.CraftingProductionTabButton;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;
import com.uncreated.civilized.ui.menu.building.BuildingSettingsTab;
import com.uncreated.civilized.ui.menu.building.residence.tabs.ManageResidentsTab;
import com.uncreated.civilized.ui.menu.building.residence.tabs.ResidenceInfoTab;
import com.uncreated.civilized.ui.tabs.ATab;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CraftsmanHouseBuildingScreen extends ABuildingMenuScreen {

   public CraftsmanHouseBuildingScreen(BuildingMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
   }

   @Override
   protected ATab createDefaultTab(ITabHost tabHost) {
      return new ResidenceInfoTab(tabHost, font, context);
   }

   @Override
   public List<Button> createTabButtons(ITabHost tabHost) {
      return List.of(
            new HomeTabButton(tabHost, 0, this::createDefaultTab),
            new CraftingProductionTabButton(
                  tabHost,
                  1,
                  host -> new ManageProductionBillsTab(
                        host,
                        font,
                        context,
                        ProductionTypes.CRAFTING,
                        EditProductionBillLayout.CRAFTING,
                        this)),
            new ManageResidentsTabButton(tabHost, 2, host -> new ManageResidentsTab(host, font, context)),
            new SettingsTabButton(tabHost, 3, host -> new BuildingSettingsTab(host, font, context)));
   }
}
