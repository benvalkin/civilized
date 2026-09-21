package com.uncreated.civilized.neoforge.registration.gui;

import com.uncreated.civilized.ui.menu.building.residence.artisan.crafting.EditCraftingMenuScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.crafting.EditCraftingRecipeMenu;
import com.uncreated.civilized.ui.menu.building.residence.artisan.singleitem.EditBlastingRecipeMenu;
import com.uncreated.civilized.ui.menu.building.residence.artisan.singleitem.EditBlastingRecipeScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.singleitem.EditSmeltingRecipeMenu;
import com.uncreated.civilized.ui.menu.building.residence.artisan.singleitem.EditSmeltingRecipeScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.singleitem.EditSmokingRecipeMenu;
import com.uncreated.civilized.ui.menu.building.residence.artisan.singleitem.EditSmokingRecipeScreen;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class GuiSetupEvents {

   @SubscribeEvent
   public static void registerScreens(RegisterMenuScreensEvent event) {
      // every building screen shares this one menu type, so the building's type decides which screen it gets
      event.register(
            GuiRegistry.BUILDING_MENU.get(),
            (MenuScreens.ScreenConstructor<BuildingMenu, ABuildingMenuScreen>) (buildingMenu, inventory, component) -> {
               BuildingType buildingType = buildingMenu.getBuilding().getBuildingType();
               return buildingType.buildingMenuScreenSupplier()
                     .create(buildingMenu, inventory, buildingType.translationDark());
            });

      event.register(
            GuiRegistry.EDIT_CRAFTING_RECIPE_MENU.get(), // do not remove cast - it seems to cause compile errors even
            // though
            // intellij thinks its redundant
            (MenuScreens.ScreenConstructor<EditCraftingRecipeMenu, EditCraftingMenuScreen>) (
                  buildingMenu,
                  inventory,
                  component) -> new EditCraftingMenuScreen(
                        buildingMenu,
                        inventory,
                        Component.translatable(
                              "menu.building.residence.production_bills.edit_crafting_recipe.description")));

      event.register(
            GuiRegistry.EDIT_SMELTING_RECIPE_MENU.get(), // do not remove cast - it seems to cause compile errors even
            // though
            // intellij thinks its redundant
            (MenuScreens.ScreenConstructor<EditSmeltingRecipeMenu, EditSmeltingRecipeScreen>) (
                  buildingMenu,
                  inventory,
                  component) -> new EditSmeltingRecipeScreen(
                        buildingMenu,
                        inventory,
                        Component.translatable(
                              "menu.building.residence.production_bills.edit_smelting_recipe.description")));

      event.register(
            GuiRegistry.EDIT_SMOKING_RECIPE_MENU.get(), // do not remove cast - it seems to cause compile errors even
            // though
            // intellij thinks its redundant
            (MenuScreens.ScreenConstructor<EditSmokingRecipeMenu, EditSmokingRecipeScreen>) (
                  buildingMenu,
                  inventory,
                  component) -> new EditSmokingRecipeScreen(
                        buildingMenu,
                        inventory,
                        Component.translatable(
                              "menu.building.residence.production_bills.edit_smoking_recipe.description")));

      event.register(
            GuiRegistry.EDIT_BLASTING_RECIPE_MENU.get(), // do not remove cast - it seems to cause compile errors even
            // though
            // intellij thinks its redundant
            (MenuScreens.ScreenConstructor<EditBlastingRecipeMenu, EditBlastingRecipeScreen>) (
                  buildingMenu,
                  inventory,
                  component) -> new EditBlastingRecipeScreen(
                        buildingMenu,
                        inventory,
                        Component.translatable(
                              "menu.building.residence.production_bills.edit_blasting_recipe.description")));
   }
}
