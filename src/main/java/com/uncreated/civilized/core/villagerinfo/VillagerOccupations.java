package com.uncreated.civilized.core.villagerinfo;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.entity.behaviour.worker.WorkActivities;

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
public class VillagerOccupations {

   private static final ResourceKey<Registry<VillagerOccupation>> OCCUPATION_TYPES_KEY =
         ResourceKey
               .createRegistryKey(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "villager_occupation_types"));
   private static final Registry<VillagerOccupation> OCCUPATION_TYPES_INTERNAL =
         new RegistryBuilder<>(OCCUPATION_TYPES_KEY).create();

   public static final DeferredRegister<VillagerOccupation> OCCUPATION_TYPES =
         DeferredRegister.create(OCCUPATION_TYPES_INTERNAL, CIVILIZED_MOD_ID);

   private static void registerOccupation(
         RegisterEvent.RegisterHelper<VillagerOccupation> registry,
         VillagerOccupation occupation) {
      registry.register(occupation.resourceLocation(), occupation);
   }

   public static ResourceLocation createResourceKey(String villagerOccupationName) {
      return ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, villagerOccupationName);
   }

   public static VillagerOccupation getFromResourceLocation(ResourceLocation resourceLocation) {
      return OCCUPATION_TYPES.getRegistry().get().getValue(resourceLocation);
   }

   @SubscribeEvent
   private static void registerRegistry(NewRegistryEvent event) {
      event.register(OCCUPATION_TYPES_INTERNAL);
   }

   @SubscribeEvent
   private static void registerTypes(RegisterEvent event) {
      event.register(OCCUPATION_TYPES_KEY, registry -> {
         registerOccupation(registry, UNEMPLOYED);
         registerOccupation(registry, PRIEST);
         registerOccupation(registry, SOLDIER);
         registerOccupation(registry, TAVERN_KEEPER);
         registerOccupation(registry, FARMER);
         registerOccupation(registry, MINER);
         registerOccupation(registry, WOODCUTTER);
         registerOccupation(registry, STONECUTTER);
         registerOccupation(registry, RANCHER);
         registerOccupation(registry, FISHERMAN);
         registerOccupation(registry, BEEKEEPER);
         registerOccupation(registry, BAKER);
         registerOccupation(registry, BUTCHER);
         registerOccupation(registry, BLACKSMITH);
         registerOccupation(registry, MASON);
         registerOccupation(registry, CARPENTER);
         registerOccupation(registry, TOOLSMITH);
         registerOccupation(registry, WEAPONSMITH);
         registerOccupation(registry, ARMORER);
         registerOccupation(registry, FLETCHER);
         registerOccupation(registry, LEATHERWORKER);
         registerOccupation(registry, WEAVER);
         registerOccupation(registry, CARTOGRAPHER);
         registerOccupation(registry, ARTIST);
      });
   }

   public static final VillagerOccupation UNEMPLOYED =
         VillagerOccupation.builder(createResourceKey("unemployed")).build();
   // resource gatherer
   public static final VillagerOccupation FARMER =
         VillagerOccupation.builder(createResourceKey("farmer"))
               .homeType(BuildingTypes.FARMER_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.CROP_FARM))
               .workBehaviourPackage(WorkActivities::getFarmerWorkPackage)
               .build();
   public static final VillagerOccupation RANCHER =
         VillagerOccupation.builder(createResourceKey("rancher"))
               .homeType(BuildingTypes.RANCHER_HOUSE)
               .validWorksite(BuildingType::isAnimalFarm)
               .workBehaviourPackage(WorkActivities::getRancherWorkPackage)
               .build();
   public static final VillagerOccupation WOODCUTTER =
         VillagerOccupation.builder(createResourceKey("woodcutter"))
               .homeType(BuildingTypes.WOODCUTTER_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.GROVE))
               .workBehaviourPackage(WorkActivities::getWoodcutterWorkPackage)
               .build();
   public static final VillagerOccupation STONECUTTER =
         VillagerOccupation.builder(createResourceKey("stonecutter"))
               .homeType(BuildingTypes.STONECUTTER_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.QUARRY))
               .build();
   public static final VillagerOccupation MINER =
         VillagerOccupation.builder(createResourceKey("miner"))
               .homeType(BuildingTypes.MINER_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.MINE))
               .workBehaviourPackage(WorkActivities::getMinerWorkPackage)
               .build();
   public static final VillagerOccupation BEEKEEPER =
         VillagerOccupation.builder(createResourceKey("beekeeper"))
               .homeType(BuildingTypes.BEEKEEPER_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.BEE_FARM))
               .workBehaviourPackage(WorkActivities::getBeekeeperWorkPackage)
               .build();
   public static final VillagerOccupation FISHERMAN =
         VillagerOccupation.builder(createResourceKey("fisherman"))
               .homeType(BuildingTypes.FISHERMAN_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.FISHING_SPOT))
               .workBehaviourPackage(WorkActivities::getFishermanWorkPackage)
               .build();
   // artisan
   public static final VillagerOccupation BAKER =
         VillagerOccupation.builder(createResourceKey("baker"))
               .homeType(BuildingTypes.BAKER_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.BAKER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation BUTCHER =
         VillagerOccupation.builder(createResourceKey("butcher"))
               .homeType(BuildingTypes.BUTCHER_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.BUTCHER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation BLACKSMITH =
         VillagerOccupation.builder(createResourceKey("blacksmith"))
               .homeType(BuildingTypes.BLACKSMITH_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.BLACKSMITH_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation TOOLSMITH =
         VillagerOccupation.builder(createResourceKey("toolsmith"))
               .homeType(BuildingTypes.TOOLSMITH_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.TOOLSMITH_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation WEAPONSMITH =
         VillagerOccupation.builder(createResourceKey("weaponsmith"))
               .homeType(BuildingTypes.WEAPONSMITH_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.WEAPONSMITH_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation ARMORER =
         VillagerOccupation.builder(createResourceKey("armorer"))
               .homeType(BuildingTypes.ARMORER_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.ARMORER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation FLETCHER =
         VillagerOccupation.builder(createResourceKey("fletcher"))
               .homeType(BuildingTypes.FLETCHER_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.FLETCHER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation CARPENTER =
         VillagerOccupation.builder(createResourceKey("carpenter"))
               .homeType(BuildingTypes.CARPENTER_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.CARPENTER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation MASON =
         VillagerOccupation.builder(createResourceKey("mason"))
               .homeType(BuildingTypes.MASON_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.MASON_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation LEATHERWORKER =
         VillagerOccupation.builder(createResourceKey("leatherworker"))
               .homeType(BuildingTypes.LEATHERWORKER_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.LEATHERWORKER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation WEAVER =
         VillagerOccupation.builder(createResourceKey("weaver"))
               .homeType(BuildingTypes.WEAVER_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.WEAVER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation CARTOGRAPHER =
         VillagerOccupation.builder(createResourceKey("cartographer"))
               .homeType(BuildingTypes.CARTOGRAPHER_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.CARTOGRAPHER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation ARTIST =
         VillagerOccupation.builder(createResourceKey("artist"))
               .homeType(BuildingTypes.ARTIST_HOUSE)
               .validWorksite(b -> b.is(BuildingTypes.ARTIST_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   // special
   public static final VillagerOccupation TAVERN_KEEPER =
         VillagerOccupation.builder(createResourceKey("tavern_keeper"))
               .homeType(BuildingTypes.TAVERN)
               .validWorksite(b -> b.is(BuildingTypes.TAVERN))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation PRIEST =
         VillagerOccupation.builder(createResourceKey("priest"))
               .homeType(BuildingTypes.CHURCH)
               .validWorksite(b -> b.is(BuildingTypes.CHURCH))
               .build();
   public static final VillagerOccupation SOLDIER =
         VillagerOccupation.builder(createResourceKey("soldier"))
               .homeType(BuildingTypes.BARRACKS)
               .validWorksite(b -> b.is(BuildingTypes.BARRACKS) || b.is(BuildingTypes.GUARD_POST))
               .workBehaviourPackage(WorkActivities::getGuardWorkPackage)
               .build();
}
