package com.uncreated.civilized.core.villagerinfo;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.entity.behaviour.worker.WorkActivities;
import com.uncreated.civilized.entity.stats.cultures.CommonClothingSets;

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

   private static @NotNull List<String> labourerClothing() {
      return List.of(CommonClothingSets.LABOURER);
   }

   private static @NotNull List<String> labourerAndCitizenClothing() {
      return List.of(CommonClothingSets.LABOURER, CommonClothingSets.PEASANT);
   }

   public static final VillagerOccupation UNEMPLOYED =
         VillagerOccupation.builder(createResourceKey("unemployed")).build();
   // resource gatherer
   public static final VillagerOccupation FARMER =
         VillagerOccupation.builder(createResourceKey("farmer"))
               .homeType(BuildingTypes.FARMER_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.CROP_FARM))
               .workBehaviourPackage(WorkActivities::getFarmerWorkPackage)
               .clothingSets(labourerAndCitizenClothing())
               .build();
   public static final VillagerOccupation RANCHER =
         VillagerOccupation.builder(createResourceKey("rancher"))
               .homeType(BuildingTypes.RANCHER_HOUSE)
               .worksiteTypes(
                     List.of(
                           BuildingTypes.COW_FARM,
                           BuildingTypes.SHEEP_FARM,
                           BuildingTypes.PIG_FARM,
                           BuildingTypes.CHICKEN_FARM))
               .workBehaviourPackage(WorkActivities::getRancherWorkPackage)
               .clothingSets(labourerClothing())
               .build();

   public static final VillagerOccupation WOODCUTTER =
         VillagerOccupation.builder(createResourceKey("woodcutter"))
               .clothingSets(List.of("labourer"))
               .homeType(BuildingTypes.WOODCUTTER_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.GROVE))
               .workBehaviourPackage(WorkActivities::getWoodcutterWorkPackage)
               .clothingSets(labourerClothing())
               .build();
   public static final VillagerOccupation STONECUTTER =
         VillagerOccupation.builder(createResourceKey("stonecutter"))
               .clothingSets(List.of("labourer"))
               .homeType(BuildingTypes.STONECUTTER_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.QUARRY))
               .clothingSets(labourerClothing())
               .build();
   public static final VillagerOccupation MINER =
         VillagerOccupation.builder(createResourceKey("miner"))
               .clothingSets(List.of("labourer"))
               .homeType(BuildingTypes.MINER_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.MINE))
               .workBehaviourPackage(WorkActivities::getMinerWorkPackage)
               .clothingSets(labourerClothing())
               .build();
   public static final VillagerOccupation BEEKEEPER =
         VillagerOccupation.builder(createResourceKey("beekeeper"))
               .homeType(BuildingTypes.BEEKEEPER_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.BEE_FARM))
               .workBehaviourPackage(WorkActivities::getBeekeeperWorkPackage)
               .clothingSets(labourerClothing())
               .build();
   public static final VillagerOccupation FISHERMAN =
         VillagerOccupation.builder(createResourceKey("fisherman"))
               .homeType(BuildingTypes.FISHERMAN_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.FISHING_SPOT))
               .workBehaviourPackage(WorkActivities::getFishermanWorkPackage)
               .clothingSets(labourerClothing())
               .build();
   // artisan
   public static final VillagerOccupation BAKER =
         VillagerOccupation.builder(createResourceKey("baker"))
               .homeType(BuildingTypes.BAKER_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.BAKER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation BUTCHER =
         VillagerOccupation.builder(createResourceKey("butcher"))
               .homeType(BuildingTypes.BUTCHER_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.BUTCHER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .clothingSets(labourerClothing())
               .build();
   public static final VillagerOccupation BLACKSMITH =
         VillagerOccupation.builder(createResourceKey("blacksmith"))
               .clothingSets(List.of("labourer"))
               .homeType(BuildingTypes.BLACKSMITH_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.BLACKSMITH_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .clothingSets(labourerClothing())
               .build();
   public static final VillagerOccupation TOOLSMITH =
         VillagerOccupation.builder(createResourceKey("toolsmith"))
               .clothingSets(List.of("labourer"))
               .homeType(BuildingTypes.TOOLSMITH_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.TOOLSMITH_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .clothingSets(labourerClothing())
               .build();
   public static final VillagerOccupation WEAPONSMITH =
         VillagerOccupation.builder(createResourceKey("weaponsmith"))
               .clothingSets(List.of("labourer"))
               .homeType(BuildingTypes.WEAPONSMITH_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.WEAPONSMITH_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .clothingSets(labourerClothing())
               .build();
   public static final VillagerOccupation ARMORER =
         VillagerOccupation.builder(createResourceKey("armorer"))
               .clothingSets(List.of("labourer"))
               .homeType(BuildingTypes.ARMORER_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.ARMORER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .clothingSets(labourerClothing())
               .build();
   public static final VillagerOccupation FLETCHER =
         VillagerOccupation.builder(createResourceKey("fletcher"))
               .homeType(BuildingTypes.FLETCHER_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.FLETCHER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .clothingSets(labourerClothing())
               .build();
   public static final VillagerOccupation CARPENTER =
         VillagerOccupation.builder(createResourceKey("carpenter"))
               .clothingSets(List.of("labourer"))
               .homeType(BuildingTypes.CARPENTER_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.CARPENTER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .clothingSets(labourerClothing())
               .build();
   public static final VillagerOccupation MASON =
         VillagerOccupation.builder(createResourceKey("mason"))
               .clothingSets(List.of("labourer"))
               .homeType(BuildingTypes.MASON_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.MASON_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .clothingSets(labourerClothing())
               .build();
   public static final VillagerOccupation LEATHERWORKER =
         VillagerOccupation.builder(createResourceKey("leatherworker"))
               .homeType(BuildingTypes.LEATHERWORKER_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.LEATHERWORKER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .clothingSets(labourerClothing())
               .build();
   public static final VillagerOccupation WEAVER =
         VillagerOccupation.builder(createResourceKey("weaver"))
               .homeType(BuildingTypes.WEAVER_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.WEAVER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation CARTOGRAPHER =
         VillagerOccupation.builder(createResourceKey("cartographer"))
               .homeType(BuildingTypes.CARTOGRAPHER_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.CARTOGRAPHER_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation ARTIST =
         VillagerOccupation.builder(createResourceKey("artist"))
               .homeType(BuildingTypes.ARTIST_HOUSE)
               .worksiteTypes(List.of(BuildingTypes.ARTIST_HOUSE))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   // special
   public static final VillagerOccupation TAVERN_KEEPER =
         VillagerOccupation.builder(createResourceKey("tavern_keeper"))
               .homeType(BuildingTypes.TAVERN)
               .worksiteTypes(List.of(BuildingTypes.TAVERN))
               .workBehaviourPackage(WorkActivities::getArtisanWorkPackage)
               .build();
   public static final VillagerOccupation PRIEST =
         VillagerOccupation.builder(createResourceKey("priest"))
               .homeType(BuildingTypes.CHURCH)
               .worksiteTypes(List.of(BuildingTypes.CHURCH))
               .build();
   public static final VillagerOccupation SOLDIER =
         VillagerOccupation.builder(createResourceKey("soldier"))
               .homeType(BuildingTypes.BARRACKS)
               .worksiteTypes(List.of(BuildingTypes.BARRACKS, BuildingTypes.GUARD_POST))
               .workBehaviourPackage(WorkActivities::getGuardWorkPackage)
               .build();
}
