package com.uncreated.civilized.neoforge.registration.gui;

import com.uncreated.civilized.ui.menu.building.residence.artisan.crafting.EditCraftingMenuScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.crafting.EditCraftingRecipeMenu;
import com.uncreated.civilized.ui.menu.building.residence.artisan.singleitem.EditBlastingRecipeMenu;
import com.uncreated.civilized.ui.menu.building.residence.artisan.singleitem.EditBlastingRecipeScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.singleitem.EditSmeltingRecipeMenu;
import com.uncreated.civilized.ui.menu.building.residence.artisan.singleitem.EditSmeltingRecipeScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.singleitem.EditSmokingRecipeMenu;
import com.uncreated.civilized.ui.menu.building.residence.artisan.singleitem.EditSmokingRecipeScreen;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;
import com.uncreated.civilized.ui.menu.building.worksite.animalfarm.AnimalFarmBuildingScreen;
import com.uncreated.civilized.ui.menu.building.worksite.cropfarm.items.ChooseCropsMenu;
import com.uncreated.civilized.ui.menu.building.worksite.cropfarm.items.ChooseCropsScreen;
import com.uncreated.civilized.ui.menu.building.worksite.grove.items.ChooseSaplingsMenu;
import com.uncreated.civilized.ui.menu.building.worksite.grove.items.ChooseSaplingsScreen;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class GuiSetupEvents {

   @SubscribeEvent
   public static void registerScreens(RegisterMenuScreensEvent event) {
      // every building screen shares this one menu type. While the screens are being moved over to it, only the animal
      // farms use it, so the animal farm screen is the only one it can create
      event.register(
            GuiRegistry.BUILDING_MENU.get(),
            (MenuScreens.ScreenConstructor<BuildingMenu, AnimalFarmBuildingScreen>) (
                  buildingMenu,
                  inventory,
                  component) -> new AnimalFarmBuildingScreen(
                        buildingMenu,
                        inventory,
                        buildingMenu.getBuilding().getBuildingType().translationDark()));

      event.register(
            GuiRegistry.CHOOSE_CROPS_MENU.get(), // do not remove cast - it seems to cause compile errors even though
            // intellij thinks its redundant
            (MenuScreens.ScreenConstructor<ChooseCropsMenu, ChooseCropsScreen>) (
                  buildingMenu,
                  inventory,
                  component) -> new ChooseCropsScreen(
                        buildingMenu,
                        inventory,
                        Component.translatable("menu.building.worksite.crop_farm.allowed_crops.description")));

      event.register(
            GuiRegistry.CHOOSE_SAPLINGS_MENU.get(), // do not remove cast - it seems to cause compile errors even though
            // intellij thinks its redundant
            (MenuScreens.ScreenConstructor<ChooseSaplingsMenu, ChooseSaplingsScreen>) (
                  buildingMenu,
                  inventory,
                  component) -> new ChooseSaplingsScreen(
                        buildingMenu,
                        inventory,
                        Component.translatable("menu.building.worksite.grove.allowed_saplings.description")));

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
