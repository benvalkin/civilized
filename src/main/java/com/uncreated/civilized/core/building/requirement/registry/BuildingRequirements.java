package com.uncreated.civilized.core.building.requirement.registry;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.requirement.CurrencyRequirement;
import com.uncreated.civilized.core.building.requirement.EnclosedRoomRequirement;
import com.uncreated.civilized.core.building.requirement.FishingSiteWaterRequirement;
import com.uncreated.civilized.core.building.requirement.PopulationRequirement;
import com.uncreated.civilized.core.building.requirement.SpaceRequirement;
import com.uncreated.civilized.core.building.requirement.SurfaceAreaRequirement;
import com.uncreated.civilized.core.building.requirement.blockcount.BlockTypeRequirement;
import com.uncreated.civilized.core.building.requirement.blockcount.BuildingBlockTypes;
import com.uncreated.civilized.core.building.requirement.blockcount.specific.BedsPresentRequirement;
import com.uncreated.civilized.core.building.requirement.blockcount.specific.BeehivesPresentRequirement;
import com.uncreated.civilized.core.building.requirement.blockcount.specific.BlastFurnacesPresentRequirement;
import com.uncreated.civilized.core.building.requirement.blockcount.specific.ChestsPresentRequirement;
import com.uncreated.civilized.core.building.requirement.blockcount.specific.CraftingTablesPresentRequirement;
import com.uncreated.civilized.core.building.requirement.blockcount.specific.FurnacesPresentRequirement;
import com.uncreated.civilized.core.building.requirement.blockcount.specific.SignsPresentRequirement;
import com.uncreated.civilized.core.building.requirement.blockcount.specific.SmokersPresentRequirement;

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
public class BuildingRequirements {

   private static final ResourceKey<Registry<BuildingRequirementList>> BUILDING_REQUIREMENTS_KEY =
         ResourceKey
               .createRegistryKey(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "building_requirements"));
   private static final Registry<BuildingRequirementList> BUILDING_REQUIREMENTS_INTERNAL =
         new RegistryBuilder<>(BUILDING_REQUIREMENTS_KEY).create();

   public static final DeferredRegister<BuildingRequirementList> BUILDING_REQUIREMENTS =
         DeferredRegister.create(BUILDING_REQUIREMENTS_INTERNAL, CIVILIZED_MOD_ID);

   @SubscribeEvent
   private static void registerRegistries(NewRegistryEvent event) {
      event.register(BUILDING_REQUIREMENTS_INTERNAL);
   }

   @SubscribeEvent
   private static void registerRequirements(RegisterEvent event) {
      event.register(BUILDING_REQUIREMENTS_KEY, registry -> {

         registerStoreHouseLevel(registry, 6, 1, b -> {
         });
         registerStoreHouseLevel(registry, 10, 2, b -> {
            b.add(new CurrencyRequirement(500));
         });
         registerStoreHouseLevel(registry, 16, 3, b -> {
            b.add(new CurrencyRequirement(900));
         });

         registerRequirements(
               registry,
               BuildingRequirementList.forBuilding(BuildingTypes.INN, 1)
                     .add(new SpaceRequirement(50))
                     .add(new EnclosedRoomRequirement())
                     .add(new BlockTypeRequirement(BuildingBlockTypes.WOOD, 200, 200))
                     .add(new ChestsPresentRequirement(1, false))
                     .add(new BedsPresentRequirement(4, 4, false))
                     .add(new SignsPresentRequirement(1, false))
                     .create());

         registerRequirements(
               registry,
               BuildingRequirementList.forBuilding(BuildingTypes.GUARD_POST, 1)
                     .add(new EnclosedRoomRequirement())
                     .add(new SpaceRequirement(2))
                     .add(new ChestsPresentRequirement(1, false))
                     .add(new SignsPresentRequirement(1, false))
                     .create());

         // civic
         registerTownHall(registry);

         // an open space for visitors to arrive in, so there are no walls or space requirements
         registerRequirements(
               registry,
               BuildingRequirementList.forBuilding(BuildingTypes.TOWN_SQUARE, 1)
                     .add(new SurfaceAreaRequirement(64))
                     .add(new SignsPresentRequirement(1, false))
                     .create());

         // special residences
         registerRequirements(
               registry,
               BuildingRequirementList.forBuilding(BuildingTypes.CHURCH, 1)
                     .add(new SpaceRequirement(50))
                     .add(new EnclosedRoomRequirement())
                     .add(new BlockTypeRequirement(BuildingBlockTypes.WOOD, 80))
                     .add(new BlockTypeRequirement(BuildingBlockTypes.STONE, 100))
                     .add(new ChestsPresentRequirement(1, false))
                     .add(new BedsPresentRequirement(1, false))
                     .add(new SignsPresentRequirement(1, false))
                     .create());

         registerRequirements(
               registry,
               BuildingRequirementList.forBuilding(BuildingTypes.TAVERN, 1)
                     .add(new SpaceRequirement(50))
                     .add(new EnclosedRoomRequirement())
                     .add(new BlockTypeRequirement(BuildingBlockTypes.WOOD, 200))
                     .add(new ChestsPresentRequirement(2, false))
                     .add(new BedsPresentRequirement(1, false))
                     .add(new SignsPresentRequirement(1, false))
                     .create());

         registerBarracks(registry);

         registerStandardHouse(BuildingTypes.TOWN_HOUSE, registry, b -> {
         });

         registerStandardHouse(BuildingTypes.MILITARY_HOUSE, registry, b -> {
         });

         // resource gatherer houses. Their work happens at their worksites, so they don't need workstations
         registerStandardHouse(BuildingTypes.FARMER_HOUSE, registry, b -> {
         });

         registerStandardHouse(BuildingTypes.WOODCUTTER_HOUSE, registry, b -> {
         });

         registerStandardHouse(BuildingTypes.MINER_HOUSE, registry, b -> {
         });

         registerStandardHouse(BuildingTypes.STONECUTTER_HOUSE, registry, b -> {
         });

         registerStandardHouse(BuildingTypes.RANCHER_HOUSE, registry, b -> {
         });

         // artisan houses, which need a workstation for each kind of production they support
         registerStandardHouse(BuildingTypes.BAKER_HOUSE, registry, b -> {
            b.add(new CraftingTablesPresentRequirement(1, 1, false));
            b.add(new FurnacesPresentRequirement(1, false));
         });

         registerStandardHouse(BuildingTypes.BUTCHER_HOUSE, registry, b -> {
            b.add(new CraftingTablesPresentRequirement(1, 1, false));
            b.add(new SmokersPresentRequirement(1, 1, false));
         });

         registerStandardHouse(BuildingTypes.BLACKSMITH_HOUSE, registry, b -> {
            b.add(new CraftingTablesPresentRequirement(1, 1, false));
            b.add(new BlastFurnacesPresentRequirement(1, 1, false));
         });

         registerStandardHouse(BuildingTypes.MASON_HOUSE, registry, b -> {
            b.add(new CraftingTablesPresentRequirement(1, false));
            b.add(new FurnacesPresentRequirement(1, false));
         });

         for (BuildingType craftingOnlyHouse : List.of(
               BuildingTypes.CARPENTER_HOUSE,
               BuildingTypes.TOOLSMITH_HOUSE,
               BuildingTypes.WEAPONSMITH_HOUSE,
               BuildingTypes.ARMORER_HOUSE,
               BuildingTypes.LEATHERWORKER_HOUSE,
               BuildingTypes.WEAVER_HOUSE,
               BuildingTypes.FLETCHER_HOUSE,
               BuildingTypes.CARTOGRAPHER_HOUSE,
               BuildingTypes.ARTIST_HOUSE)) {
            registerStandardHouse(craftingOnlyHouse, registry, b -> {
               b.add(new CraftingTablesPresentRequirement(1, false));
            });
         }

         registerRequirements(
               registry,
               BuildingRequirementList.forBuilding(BuildingTypes.CROP_FARM, 1)
                     .add(new SurfaceAreaRequirement(40))
                     .add(new SignsPresentRequirement(1, false))
                     .create());

         registerRequirements(
               registry,
               BuildingRequirementList.forBuilding(BuildingTypes.GROVE, 1)
                     .add(new SurfaceAreaRequirement(80))
                     .add(new SignsPresentRequirement(1, false))
                     .create());

         registerRequirements(
               registry,
               BuildingRequirementList.forBuilding(BuildingTypes.COW_FARM, 1)
                     .add(new SurfaceAreaRequirement(64))
                     .add(new SignsPresentRequirement(1, false))
                     .create());

         registerStandardHouse(BuildingTypes.BEEKEEPER_HOUSE, registry, b -> {
         });

         registerStandardHouse(BuildingTypes.FISHERMAN_HOUSE, registry, b -> {
         });

         registerRequirements(
               registry,
               BuildingRequirementList.forBuilding(BuildingTypes.FISHING_SPOT, 1)
                     .add(new FishingSiteWaterRequirement(5, 5, 4))
                     .add(new SignsPresentRequirement(1, false))
                     .create());

         registerRequirements(
               registry,
               BuildingRequirementList.forBuilding(BuildingTypes.BEE_FARM, 1)
                     .add(new BeehivesPresentRequirement(1, 4, false))
                     .add(new SignsPresentRequirement(1, false))
                     .create());

         registerRequirements(
               registry,
               BuildingRequirementList.forBuilding(BuildingTypes.MINE, 1)
                     .add(new SurfaceAreaRequirement(9))
                     .add(new SignsPresentRequirement(1, false))
                     .create());
      });
   }

   private static void registerBarracks(RegisterEvent.RegisterHelper<BuildingRequirementList> registry) {
      registerRequirements(
            registry,
            BuildingRequirementList.forBuilding(BuildingTypes.BARRACKS, 1)
                  .add(new SpaceRequirement(40))
                  .add(new EnclosedRoomRequirement())
                  .add(new BlockTypeRequirement(BuildingBlockTypes.WOOD, 120))
                  .add(new BlockTypeRequirement(BuildingBlockTypes.STONE, 64))
                  .add(new ChestsPresentRequirement(2, false))
                  .add(new BedsPresentRequirement(4, 4, false))
                  .add(new SignsPresentRequirement(1, false))
                  .add(new CurrencyRequirement(100))
                  .create());

      registerRequirements(
            registry,
            BuildingRequirementList.forBuilding(BuildingTypes.BARRACKS, 2)
                  .add(new SpaceRequirement(50))
                  .add(new EnclosedRoomRequirement())
                  .add(new BlockTypeRequirement(BuildingBlockTypes.WOOD, 120))
                  .add(new BlockTypeRequirement(BuildingBlockTypes.STONE, 120))
                  .add(new ChestsPresentRequirement(2, false))
                  .add(new BedsPresentRequirement(6, 6, false))
                  .add(new SignsPresentRequirement(1, false))
                  .add(new CurrencyRequirement(400))
                  .create());

      registerRequirements(
            registry,
            BuildingRequirementList.forBuilding(BuildingTypes.BARRACKS, 3)
                  .add(new SpaceRequirement(60))
                  .add(new EnclosedRoomRequirement())
                  .add(new BlockTypeRequirement(BuildingBlockTypes.WOOD, 100))
                  .add(new BlockTypeRequirement(BuildingBlockTypes.STONE, 250))
                  .add(new ChestsPresentRequirement(2, false))
                  .add(new BedsPresentRequirement(8, 8, false))
                  .add(new SignsPresentRequirement(1, false))
                  .add(new CurrencyRequirement(800))
                  .create());
   }

   private static void registerTownHall(RegisterEvent.RegisterHelper<BuildingRequirementList> registry) {
      registerRequirements(
            registry,
            BuildingRequirementList.forBuilding(BuildingTypes.TOWN_HALL, 1)
                  .add(new SpaceRequirement(40))
                  .add(new EnclosedRoomRequirement())
                  .add(new BlockTypeRequirement(BuildingBlockTypes.WOOD, 200))
                  .add(new ChestsPresentRequirement(2, false))
                  .add(new SignsPresentRequirement(1, false))
                  .create());

      registerRequirements(
            registry,
            BuildingRequirementList.forBuilding(BuildingTypes.TOWN_HALL, 2)
                  .add(new SpaceRequirement(60))
                  .add(new EnclosedRoomRequirement())
                  .add(new BlockTypeRequirement(BuildingBlockTypes.WOOD, 250))
                  .add(new BlockTypeRequirement(BuildingBlockTypes.STONE, 100))
                  .add(new ChestsPresentRequirement(2, false))
                  .add(new SignsPresentRequirement(1, false))
                  .add(new PopulationRequirement(16))
                  .add(new CurrencyRequirement(600))
                  .create());

      registerRequirements(
            registry,
            BuildingRequirementList.forBuilding(BuildingTypes.TOWN_HALL, 3)
                  .add(new SpaceRequirement(80))
                  .add(new EnclosedRoomRequirement())
                  .add(new BlockTypeRequirement(BuildingBlockTypes.WOOD, 300))
                  .add(new BlockTypeRequirement(BuildingBlockTypes.STONE, 150))
                  .add(new ChestsPresentRequirement(2, false))
                  .add(new SignsPresentRequirement(1, false))
                  .add(new PopulationRequirement(28))
                  .add(new CurrencyRequirement(1000))
                  .create());
   }

   public static BuildingRequirementList.BuildingRequirementListBuilder standardHouseL1(BuildingType buildingType) {
      return BuildingRequirementList.forBuilding(buildingType, 1)
            .add(new EnclosedRoomRequirement())
            .add(new SpaceRequirement(15))
            .add(new BlockTypeRequirement(BuildingBlockTypes.WOOD, 120))
            .add(new ChestsPresentRequirement(1, false))
            .add(new BedsPresentRequirement(1, false))
            .add(new SignsPresentRequirement(1, false));
   }

   public static BuildingRequirementList.BuildingRequirementListBuilder standardHouseL2(BuildingType buildingType) {
      return BuildingRequirementList.forBuilding(buildingType, 2)
            .add(new SpaceRequirement(30))
            .add(new EnclosedRoomRequirement())
            .add(new BlockTypeRequirement(BuildingBlockTypes.WOOD, 160))
            .add(new BlockTypeRequirement(BuildingBlockTypes.STONE, 32))
            .add(new ChestsPresentRequirement(1, false))
            .add(new BedsPresentRequirement(2, false))
            .add(new SignsPresentRequirement(1, false))
            .add(new CurrencyRequirement(300));
   }

   public static BuildingRequirementList.BuildingRequirementListBuilder standardHouseL3(BuildingType buildingType) {
      return BuildingRequirementList.forBuilding(buildingType, 3)
            .add(new SpaceRequirement(50))
            .add(new EnclosedRoomRequirement())
            .add(new BlockTypeRequirement(BuildingBlockTypes.WOOD, 160))
            .add(new BlockTypeRequirement(BuildingBlockTypes.STONE, 100))
            .add(new ChestsPresentRequirement(1, false))
            .add(new BedsPresentRequirement(2, false))
            .add(new SignsPresentRequirement(1, false))
            .add(new CurrencyRequirement(600));
   }

   public static void registerStandardHouse(
         BuildingType buildingType,
         RegisterEvent.RegisterHelper<BuildingRequirementList> registry,
         Consumer<BuildingRequirementList.BuildingRequirementListBuilder> extras) {
      registerWithExtras(standardHouseL1(buildingType), registry, extras);
      registerWithExtras(standardHouseL2(buildingType), registry, extras);
      registerWithExtras(standardHouseL3(buildingType), registry, extras);
   }

   public static void registerStoreHouseLevel(
         RegisterEvent.RegisterHelper<BuildingRequirementList> registry,
         int numberOfChests,
         int upgradeLevel,
         Consumer<BuildingRequirementList.BuildingRequirementListBuilder> extras) {
      registerWithExtras(
            BuildingRequirementList.forBuilding(BuildingTypes.STOREHOUSE, upgradeLevel)
                  .add(new SpaceRequirement(30))
                  .add(new BlockTypeRequirement(BuildingBlockTypes.WOOD, 40, 40))
                  .add(new ChestsPresentRequirement(numberOfChests, numberOfChests, false))
                  .add(new SignsPresentRequirement(1, false)),
            registry,
            extras);
   }

   public static void registerWithExtras(
         BuildingRequirementList.BuildingRequirementListBuilder builder,
         RegisterEvent.RegisterHelper<BuildingRequirementList> registry,
         Consumer<BuildingRequirementList.BuildingRequirementListBuilder> extra) {
      extra.accept(builder);
      registerRequirements(registry, builder.create());
   }

   private static ResourceLocation getResourceKey(BuildingType buildingType, int upgradeLevel) {
      return ResourceLocation
            .fromNamespaceAndPath(CIVILIZED_MOD_ID, buildingType.name().toLowerCase() + "_" + upgradeLevel);
   }

   private static BuildingRequirementList registerRequirements(
         RegisterEvent.RegisterHelper<BuildingRequirementList> registerHelper,
         BuildingRequirementList requirementList) {
      ResourceLocation resourceKey =
            getResourceKey(requirementList.getBuildingType(), requirementList.getUpgradeLevel());
      registerHelper.register(resourceKey, requirementList);
      return requirementList;
   }

   public static BuildingRequirementList getBuildingRequirements(BuildingType buildingType, int upgradeLevel) {
      Optional<BuildingRequirementList> requirements =
            BUILDING_REQUIREMENTS_INTERNAL.stream()
                  .filter(f -> f.getBuildingType() == buildingType && f.getUpgradeLevel() == upgradeLevel)
                  .findFirst();
      return requirements.orElseGet(() -> BuildingRequirementList.forBuilding(buildingType, 1).create());

   }
}
