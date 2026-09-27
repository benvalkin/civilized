package com.uncreated.civilized.neoforge.registration.gui;

import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;
import com.uncreated.civilized.ui.menu.building.BuildingScreens;

import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class GuiSetupEvents {

   @SubscribeEvent
   public static void registerScreens(RegisterMenuScreensEvent event) {
      // every building screen shares this one menu type. The building's BuildingType decides which screen it gets.
      event.register(
            GuiRegistry.BUILDING_MENU.get(),
            (MenuScreens.ScreenConstructor<BuildingMenu, ABuildingMenuScreen>) (buildingMenu, inventory,
                  component) -> BuildingScreens.create(buildingMenu, inventory));
   }
}
