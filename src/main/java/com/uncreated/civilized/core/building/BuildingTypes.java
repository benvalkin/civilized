package com.uncreated.civilized.core.building;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.core.building.entity.behaviour.ArtisanHouseBehaviour;
import com.uncreated.civilized.core.building.production.bills.ProductionTypes;
import com.uncreated.civilized.core.building.state.CropFarmState;
import com.uncreated.civilized.core.building.state.GroveState;
import com.uncreated.civilized.core.building.state.animalfarm.BeeFarmState;
import com.uncreated.civilized.core.building.state.animalfarm.ChickenFarmState;
import com.uncreated.civilized.core.building.state.animalfarm.CowFarmState;
import com.uncreated.civilized.core.building.state.animalfarm.PigFarmState;
import com.uncreated.civilized.core.building.state.animalfarm.SheepFarmState;
import com.uncreated.civilized.core.building.state.artisan.ArmorerHouseState;
import com.uncreated.civilized.core.building.state.artisan.ArtistHouseState;
import com.uncreated.civilized.core.building.state.artisan.BakeryState;
import com.uncreated.civilized.core.building.state.artisan.BlacksmithHouseState;
import com.uncreated.civilized.core.building.state.artisan.ButcheryState;
import com.uncreated.civilized.core.building.state.artisan.CarpenterHouseState;
import com.uncreated.civilized.core.building.state.artisan.CartographerHouseState;
import com.uncreated.civilized.core.building.state.artisan.FletcherHouseState;
import com.uncreated.civilized.core.building.state.artisan.LeatherworkerHouseState;
import com.uncreated.civilized.core.building.state.artisan.MasonHouseState;
import com.uncreated.civilized.core.building.state.artisan.ToolsmithHouseState;
import com.uncreated.civilized.core.building.state.artisan.WeaponsmithHouseState;
import com.uncreated.civilized.core.building.state.artisan.WeaverHouseState;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.ui.menu.building.inn.InnBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.BakeryBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.BlacksmithBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.ButcheryBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.CraftsmanHouseBuildingScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.MasonBuildingScreen;
import com.uncreated.civilized.ui.menu.building.worksite.WorksiteBuildingScreen;
import com.uncreated.civilized.ui.menu.building.worksite.animalfarm.AnimalFarmBuildingScreen;
import com.uncreated.civilized.ui.menu.building.worksite.cropfarm.CropFarmBuildingScreen;
import com.uncreated.civilized.ui.menu.building.worksite.grove.GroveBuildingScreen;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@EventBusSubscriber(modid = CivilizedMod.CIVILIZED_MOD_ID)
public class BuildingTypes {

   private static final ResourceKey<Registry<BuildingType>> BUILDING_TYPES_KEY =
         ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "building_types"));
   private static final Registry<BuildingType> BUILDING_TYPES_INTERNAL =
         new RegistryBuilder<>(BUILDING_TYPES_KEY).create();

   public static final DeferredRegister<BuildingType> BUILDING_TYPES =
         DeferredRegister.create(BUILDING_TYPES_INTERNAL, CIVILIZED_MOD_ID);

   private static final List<BuildingType> ALL = new ArrayList<>();

   private static BuildingType declare(BuildingType buildingType) {
      ALL.add(buildingType);
      return buildingType;
   }

   public static List<BuildingType> all() {
      return Collections.unmodifiableList(ALL);
   }

   public static ResourceLocation createResourceKey(String buildingTypeName) {
      return ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, buildingTypeName);
   }

   private static void registerBuildingType(
         RegisterEvent.RegisterHelper<BuildingType> registry,
         BuildingType buildingType) {
      registry.register(buildingType.resourceLocation(), buildingType);
   }

   public static BuildingType getFromResourceLocation(ResourceLocation resourceLocation) {
      return BuildingTypes.BUILDING_TYPES.getRegistry().get().getValue(resourceLocation);
   }

   @SubscribeEvent
   private static void registerRegistry(NewRegistryEvent event) {
      event.register(BUILDING_TYPES_INTERNAL);
   }

   @SubscribeEvent
   private static void registerTypes(RegisterEvent event) {
      event.register(
            BUILDING_TYPES_KEY,
            registry -> ALL.forEach(buildingType -> registerBuildingType(registry, buildingType)));
   }

   public static BuildingType TOWN_HALL = declare(BuildingType.builder(createResourceKey("town_hall")).build());
   public static BuildingType INN =
         declare(BuildingType.builder(createResourceKey("inn"))
               .isResidence(true)
               .buildingScreenSupplier(InnBuildingScreen::new)
               .build());
   public static BuildingType TOWN_SQUARE = declare(BuildingType.builder(createResourceKey("town_square")).build());
   public static BuildingType STOREHOUSE = declare(BuildingType.builder(createResourceKey("storehouse")).build());
   public static BuildingType CHURCH =
         declare(BuildingType.builder(createResourceKey("church"))
               .isResidence(true)
               .occupation(() -> VillagerOccupations.PRIEST)
               .build());
   public static BuildingType BARRACKS =
         declare(BuildingType.builder(createResourceKey("barracks"))
               .isResidence(true)
               .occupation(() -> VillagerOccupations.SOLDIER)
               .build());
   public static BuildingType GUARD_POST =
         declare(BuildingType.builder(createResourceKey("guard_post"))
               .isResidence(true)
               .occupation(() -> VillagerOccupations.SOLDIER)
               .build());
   public static BuildingType TAVERN =
         declare(BuildingType.builder(createResourceKey("tavern"))
               .isResidence(true)
               .occupation(() -> VillagerOccupations.TAVERN_KEEPER)
               .build());
   public static BuildingType FARMER_HOUSE =
         declare(BuildingType.builder(createResourceKey("farmer_house"))
               .isResidence(true)
               .occupation(() -> VillagerOccupations.FARMER)
               .build());
   public static BuildingType WOODCUTTER_HOUSE =
         declare(BuildingType.builder(createResourceKey("woodcutter_house"))
               .isResidence(true)
               .occupation(() -> VillagerOccupations.WOODCUTTER)
               .build());
   public static BuildingType MINER_HOUSE =
         declare(BuildingType.builder(createResourceKey("miner_house"))
               .isResidence(true)
               .occupation(() -> VillagerOccupations.MINER)
               .build());

   public static BuildingType STONECUTTER_HOUSE =
         declare(BuildingType.builder(createResourceKey("stonecutter_house"))
               .isResidence(true)
               .occupation(() -> VillagerOccupations.STONECUTTER)
               .build());

   public static BuildingType RANCHER_HOUSE =
         declare(BuildingType.builder(createResourceKey("rancher_house"))
               .isResidence(true)
               .occupation(() -> VillagerOccupations.RANCHER)
               .build());

   public static BuildingType FISHERMAN_HOUSE =
         declare(BuildingType.builder(createResourceKey("fisherman_house"))
               .isResidence(true)
               .occupation(() -> VillagerOccupations.FISHERMAN)
               .build());
   public static BuildingType BEEKEEPER_HOUSE =
         declare(BuildingType.builder(createResourceKey("beekeeper_house"))
               .isResidence(true)
               .occupation(() -> VillagerOccupations.BEEKEEPER)
               .build());

   public static BuildingType BAKER_HOUSE =
         declare(BuildingType.builder(createResourceKey("baker_house"))
               .isResidence(true)
               .supportedProductionTypes(List.of(ProductionTypes.CRAFTING, ProductionTypes.SMELTING))
               .createState(BakeryState::new)
               .createBehaviour(ArtisanHouseBehaviour::new)
               .buildingScreenSupplier(BakeryBuildingScreen::new)
               .occupation(() -> VillagerOccupations.BAKER)
               .build());

   public static BuildingType BUTCHER_HOUSE =
         declare(BuildingType.builder(createResourceKey("butcher_house"))
               .isResidence(true)
               .supportedProductionTypes(List.of(ProductionTypes.CRAFTING, ProductionTypes.SMOKING))
               .createState(ButcheryState::new)
               .createBehaviour(ArtisanHouseBehaviour::new)
               .buildingScreenSupplier(ButcheryBuildingScreen::new)
               .occupation(() -> VillagerOccupations.BUTCHER)
               .build());

   public static BuildingType BLACKSMITH_HOUSE =
         declare(BuildingType.builder(createResourceKey("blacksmith_house"))
               .isResidence(true)
               .supportedProductionTypes(List.of(ProductionTypes.CRAFTING, ProductionTypes.BLASTING))
               .createState(BlacksmithHouseState::new)
               .createBehaviour(ArtisanHouseBehaviour::new)
               .buildingScreenSupplier(BlacksmithBuildingScreen::new)
               .occupation(() -> VillagerOccupations.BLACKSMITH)
               .build());

   public static BuildingType MASON_HOUSE =
         declare(BuildingType.builder(createResourceKey("mason_house"))
               .isResidence(true)
               .supportedProductionTypes(List.of(ProductionTypes.CRAFTING, ProductionTypes.SMELTING))
               .createState(MasonHouseState::new)
               .createBehaviour(ArtisanHouseBehaviour::new)
               .buildingScreenSupplier(MasonBuildingScreen::new)
               .occupation(() -> VillagerOccupations.MASON)
               .build());

   public static BuildingType CARPENTER_HOUSE =
         declare(BuildingType.builder(createResourceKey("carpenter_house"))
               .isResidence(true)
               .supportedProductionTypes(List.of(ProductionTypes.CRAFTING))
               .createState(CarpenterHouseState::new)
               .createBehaviour(ArtisanHouseBehaviour::new)
               .buildingScreenSupplier(CraftsmanHouseBuildingScreen::new)
               .occupation(() -> VillagerOccupations.CARPENTER)
               .build());

   public static BuildingType TOOLSMITH_HOUSE =
         declare(BuildingType.builder(createResourceKey("toolsmith_house"))
               .isResidence(true)
               .supportedProductionTypes(List.of(ProductionTypes.CRAFTING))
               .createState(ToolsmithHouseState::new)
               .createBehaviour(ArtisanHouseBehaviour::new)
               .buildingScreenSupplier(CraftsmanHouseBuildingScreen::new)
               .occupation(() -> VillagerOccupations.TOOLSMITH)
               .build());

   public static BuildingType WEAPONSMITH_HOUSE =
         declare(BuildingType.builder(createResourceKey("weaponsmith_house"))
               .isResidence(true)
               .supportedProductionTypes(List.of(ProductionTypes.CRAFTING))
               .createState(WeaponsmithHouseState::new)
               .createBehaviour(ArtisanHouseBehaviour::new)
               .buildingScreenSupplier(CraftsmanHouseBuildingScreen::new)
               .occupation(() -> VillagerOccupations.WEAPONSMITH)
               .build());

   public static BuildingType ARMORER_HOUSE =
         declare(BuildingType.builder(createResourceKey("armorer_house"))
               .isResidence(true)
               .supportedProductionTypes(List.of(ProductionTypes.CRAFTING))
               .createState(ArmorerHouseState::new)
               .createBehaviour(ArtisanHouseBehaviour::new)
               .buildingScreenSupplier(CraftsmanHouseBuildingScreen::new)
               .occupation(() -> VillagerOccupations.ARMORER)
               .build());

   public static BuildingType LEATHERWORKER_HOUSE =
         declare(BuildingType.builder(createResourceKey("leatherworker_house"))
               .isResidence(true)
               .supportedProductionTypes(List.of(ProductionTypes.CRAFTING))
               .createState(LeatherworkerHouseState::new)
               .createBehaviour(ArtisanHouseBehaviour::new)
               .buildingScreenSupplier(CraftsmanHouseBuildingScreen::new)
               .occupation(() -> VillagerOccupations.LEATHERWORKER)
               .build());
   public static BuildingType WEAVER_HOUSE =
         declare(BuildingType.builder(createResourceKey("weaver_house"))
               .isResidence(true)
               .supportedProductionTypes(List.of(ProductionTypes.CRAFTING))
               .createState(WeaverHouseState::new)
               .createBehaviour(ArtisanHouseBehaviour::new)
               .buildingScreenSupplier(CraftsmanHouseBuildingScreen::new)
               .occupation(() -> VillagerOccupations.WEAVER)
               .build());

   public static BuildingType FLETCHER_HOUSE =
         declare(BuildingType.builder(createResourceKey("fletcher_house"))
               .isResidence(true)
               .supportedProductionTypes(List.of(ProductionTypes.CRAFTING))
               .createState(FletcherHouseState::new)
               .createBehaviour(ArtisanHouseBehaviour::new)
               .buildingScreenSupplier(CraftsmanHouseBuildingScreen::new)
               .occupation(() -> VillagerOccupations.FLETCHER)
               .build());

   public static BuildingType CARTOGRAPHER_HOUSE =
         declare(BuildingType.builder(createResourceKey("cartographer_house"))
               .isResidence(true)
               .supportedProductionTypes(List.of(ProductionTypes.CRAFTING))
               .createState(CartographerHouseState::new)
               .createBehaviour(ArtisanHouseBehaviour::new)
               .buildingScreenSupplier(CraftsmanHouseBuildingScreen::new)
               .occupation(() -> VillagerOccupations.CARTOGRAPHER)
               .build());

   public static BuildingType ARTIST_HOUSE =
         declare(BuildingType.builder(createResourceKey("artist_house"))
               .isResidence(true)
               .supportedProductionTypes(List.of(ProductionTypes.CRAFTING))
               .createState(ArtistHouseState::new)
               .createBehaviour(ArtisanHouseBehaviour::new)
               .buildingScreenSupplier(CraftsmanHouseBuildingScreen::new)
               .occupation(() -> VillagerOccupations.ARTIST)
               .build());

   public static BuildingType CROP_FARM =
         declare(BuildingType.builder(createResourceKey("crop_farm"))
               .isWorksite(true)
               .createState(CropFarmState::new)
               .buildingMenuScreenSupplier(CropFarmBuildingScreen::new)
               .occupation(() -> VillagerOccupations.FARMER)
               .build());

   public static BuildingType COW_FARM =
         declare(BuildingType.builder(createResourceKey("cow_farm"))
               .isWorksite(true)
               .isAnimalFarm(true)
               .createState(CowFarmState::new)
               .buildingMenuScreenSupplier(AnimalFarmBuildingScreen::new)
               .occupation(() -> VillagerOccupations.RANCHER)
               .build());

   public static BuildingType SHEEP_FARM =
         declare(BuildingType.builder(createResourceKey("sheep_farm"))
               .isWorksite(true)
               .isAnimalFarm(true)
               .createState(SheepFarmState::new)
               .buildingMenuScreenSupplier(AnimalFarmBuildingScreen::new)
               .occupation(() -> VillagerOccupations.RANCHER)
               .build());

   public static BuildingType PIG_FARM =
         declare(BuildingType.builder(createResourceKey("pig_farm"))
               .isWorksite(true)
               .isAnimalFarm(true)
               .createState(PigFarmState::new)
               .buildingMenuScreenSupplier(AnimalFarmBuildingScreen::new)
               .occupation(() -> VillagerOccupations.RANCHER)
               .build());

   public static BuildingType CHICKEN_FARM =
         declare(BuildingType.builder(createResourceKey("chicken_farm"))
               .isWorksite(true)
               .isAnimalFarm(true)
               .createState(ChickenFarmState::new)
               .buildingMenuScreenSupplier(AnimalFarmBuildingScreen::new)
               .occupation(() -> VillagerOccupations.RANCHER)
               .build());

   public static BuildingType BEE_FARM =
         declare(BuildingType.builder(createResourceKey("bee_farm"))
               .isWorksite(true)
               .isAnimalFarm(true)
               .createState(BeeFarmState::new)
               .buildingMenuScreenSupplier(AnimalFarmBuildingScreen::new)
               .occupation(() -> VillagerOccupations.BEEKEEPER)
               .build());
   public static BuildingType FISHING_SPOT =
         declare(BuildingType.builder(createResourceKey("fishing_spot"))
               .isWorksite(true)
               .buildingScreenSupplier(WorksiteBuildingScreen::new)
               .occupation(() -> VillagerOccupations.FISHERMAN)
               .build());
   public static BuildingType MINE =
         declare(BuildingType.builder(createResourceKey("mine"))
               .isWorksite(true)
               .buildingScreenSupplier(WorksiteBuildingScreen::new)
               .occupation(() -> VillagerOccupations.MINER)
               .build());
   public static BuildingType QUARRY =
         declare(BuildingType.builder(createResourceKey("quarry"))
               .isWorksite(true)
               .buildingScreenSupplier(WorksiteBuildingScreen::new)
               .occupation(() -> VillagerOccupations.STONECUTTER)
               .build());
   public static BuildingType GROVE =
         declare(BuildingType.builder(createResourceKey("grove"))
               .isWorksite(true)
               .createState(GroveState::new)
               .buildingScreenSupplier(GroveBuildingScreen::new)
               .occupation(() -> VillagerOccupations.WOODCUTTER)
               .build());
}
