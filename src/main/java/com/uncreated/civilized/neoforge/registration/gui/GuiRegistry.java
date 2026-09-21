package com.uncreated.civilized.neoforge.registration.gui;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.function.Supplier;

import com.uncreated.civilized.ui.menu.building.residence.artisan.crafting.EditCraftingRecipeMenu;
import com.uncreated.civilized.ui.menu.building.residence.artisan.singleitem.EditBlastingRecipeMenu;
import com.uncreated.civilized.ui.menu.building.residence.artisan.singleitem.EditSmeltingRecipeMenu;
import com.uncreated.civilized.ui.menu.building.residence.artisan.singleitem.EditSmokingRecipeMenu;
import com.uncreated.civilized.ui.menu.building.worksite.animalfarm.items.ChooseAnimalFoodMenu;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;
import com.uncreated.civilized.ui.menu.building.worksite.cropfarm.items.ChooseCropsMenu;
import com.uncreated.civilized.ui.menu.building.worksite.grove.items.ChooseSaplingsMenu;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public class GuiRegistry {
   public static final DeferredRegister<MenuType<?>> MENUS =
         DeferredRegister.create(BuiltInRegistries.MENU, CIVILIZED_MOD_ID);

   public static final Supplier<MenuType<BuildingMenu>> BUILDING_MENU =
         MENUS.register("building_menu", () -> IMenuTypeExtension.create(BuildingMenu::new));

   public static final Supplier<MenuType<ChooseCropsMenu>> CHOOSE_CROPS_MENU =
         MENUS.register("choose_crops_menu", () -> IMenuTypeExtension.create(ChooseCropsMenu::new));

   public static final Supplier<MenuType<ChooseSaplingsMenu>> CHOOSE_SAPLINGS_MENU =
         MENUS.register("choose_saplings_menu", () -> IMenuTypeExtension.create(ChooseSaplingsMenu::new));

   public static final Supplier<MenuType<ChooseAnimalFoodMenu>> CHOOSE_ANIMAL_FOOD_MENU =
         MENUS.register("choose_animal_food_menu", () -> IMenuTypeExtension.create(ChooseAnimalFoodMenu::new));

   public static final Supplier<MenuType<EditCraftingRecipeMenu>> EDIT_CRAFTING_RECIPE_MENU =
         MENUS.register("edit_crafting_recipe_menu", () -> IMenuTypeExtension.create(EditCraftingRecipeMenu::new));

   public static final Supplier<MenuType<EditSmeltingRecipeMenu>> EDIT_SMELTING_RECIPE_MENU =
         MENUS.register("edit_smelting_recipe_menu", () -> IMenuTypeExtension.create(EditSmeltingRecipeMenu::new));

   public static final Supplier<MenuType<EditSmokingRecipeMenu>> EDIT_SMOKING_RECIPE_MENU =
         MENUS.register("edit_smoking_recipe_menu", () -> IMenuTypeExtension.create(EditSmokingRecipeMenu::new));

   public static final Supplier<MenuType<EditBlastingRecipeMenu>> EDIT_BLASTING_RECIPE_MENU =
         MENUS.register("edit_smoking_blasting_menu", () -> IMenuTypeExtension.create(EditBlastingRecipeMenu::new));
}
