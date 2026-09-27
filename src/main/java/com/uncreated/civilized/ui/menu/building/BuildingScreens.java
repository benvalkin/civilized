package com.uncreated.civilized.ui.menu.building;

import java.util.HashMap;
import java.util.Map;

import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.ui.menu.building.inn.InnBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.ResidenceBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.BakeryBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.BlacksmithBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.ButcheryBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.CraftsmanHouseBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.MasonBuildingScreen;
import com.uncreated.civilized.ui.menu.building.townhall.TownHallBuildingScreen;
import com.uncreated.civilized.ui.menu.building.worksite.WorksiteBuildingScreen;
import com.uncreated.civilized.ui.menu.building.worksite.animalfarm.AnimalFarmBuildingScreen;
import com.uncreated.civilized.ui.menu.building.worksite.cropfarm.CropFarmBuildingScreen;
import com.uncreated.civilized.ui.menu.building.worksite.grove.GroveBuildingScreen;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.player.Inventory;

/**
 * Dedicated registry for Building Screens to avoid referencing them on dedicated servers (which would crash them).
 */
public final class BuildingScreens {

   private static final MenuScreens.ScreenConstructor<BuildingMenu, ABuildingMenuScreen> DEFAULT_SCREEN =
         ResidenceBuildingScreen::new;

   private static final Map<BuildingType, MenuScreens.ScreenConstructor<BuildingMenu, ABuildingMenuScreen>> SCREENS =
         new HashMap<>();

   static {
      register(BuildingTypes.TOWN_HALL, TownHallBuildingScreen::new);
      register(BuildingTypes.INN, InnBuildingScreen::new);

      register(BuildingTypes.BAKER_HOUSE, BakeryBuildingScreen::new);
      register(BuildingTypes.BUTCHER_HOUSE, ButcheryBuildingScreen::new);
      register(BuildingTypes.BLACKSMITH_HOUSE, BlacksmithBuildingScreen::new);
      register(BuildingTypes.MASON_HOUSE, MasonBuildingScreen::new);
      register(BuildingTypes.CARPENTER_HOUSE, CraftsmanHouseBuildingScreen::new);
      register(BuildingTypes.TOOLSMITH_HOUSE, CraftsmanHouseBuildingScreen::new);
      register(BuildingTypes.WEAPONSMITH_HOUSE, CraftsmanHouseBuildingScreen::new);
      register(BuildingTypes.ARMORER_HOUSE, CraftsmanHouseBuildingScreen::new);
      register(BuildingTypes.LEATHERWORKER_HOUSE, CraftsmanHouseBuildingScreen::new);
      register(BuildingTypes.WEAVER_HOUSE, CraftsmanHouseBuildingScreen::new);
      register(BuildingTypes.FLETCHER_HOUSE, CraftsmanHouseBuildingScreen::new);
      register(BuildingTypes.CARTOGRAPHER_HOUSE, CraftsmanHouseBuildingScreen::new);
      register(BuildingTypes.ARTIST_HOUSE, CraftsmanHouseBuildingScreen::new);

      register(BuildingTypes.CROP_FARM, CropFarmBuildingScreen::new);
      register(BuildingTypes.COW_FARM, AnimalFarmBuildingScreen::new);
      register(BuildingTypes.SHEEP_FARM, AnimalFarmBuildingScreen::new);
      register(BuildingTypes.PIG_FARM, AnimalFarmBuildingScreen::new);
      register(BuildingTypes.CHICKEN_FARM, AnimalFarmBuildingScreen::new);
      register(BuildingTypes.BEE_FARM, AnimalFarmBuildingScreen::new);
      register(BuildingTypes.FISHING_SPOT, WorksiteBuildingScreen::new);
      register(BuildingTypes.MINE, WorksiteBuildingScreen::new);
      register(BuildingTypes.QUARRY, WorksiteBuildingScreen::new);
      register(BuildingTypes.GROVE, GroveBuildingScreen::new);
   }

   private BuildingScreens() {
   }

   private static void register(
         BuildingType buildingType,
         MenuScreens.ScreenConstructor<BuildingMenu, ABuildingMenuScreen> screen) {
      SCREENS.put(buildingType, screen);
   }

   /** Building types without a screen of their own get the residence screen. */
   public static ABuildingMenuScreen create(BuildingMenu menu, Inventory inventory) {
      BuildingType buildingType = menu.getBuilding().getBuildingType();
      return SCREENS.getOrDefault(buildingType, DEFAULT_SCREEN)
            .create(menu, inventory, buildingType.translationDark());
   }
}
