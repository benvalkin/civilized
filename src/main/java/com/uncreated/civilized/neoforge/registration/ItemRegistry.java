package com.uncreated.civilized.neoforge.registration;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.item.BuildingDeedItem;
import com.uncreated.civilized.item.CurrencyItem;
import com.uncreated.civilized.item.SettlementMandateItem;
import com.uncreated.civilized.neoforge.registration.entity.EntityRegistry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegistry {
   // Create a Deferred Register to hold Items which will all be registered under the "civilized" namespace
   public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CIVILIZED_MOD_ID);

   public static final Map<BuildingType, DeferredItem<Item>> BUILDING_DEEDS = registerBuildingDeeds();

   private static Map<BuildingType, DeferredItem<Item>> registerBuildingDeeds() {
      Map<BuildingType, DeferredItem<Item>> result = new HashMap<>();
      for (BuildingType buildingType : BuildingTypes.all()) {
         result.put(buildingType, registerBuildingDeedItem(buildingType));
      }
      return result;
   }

   public static final DeferredItem<Item> SETTLEMENT_MANDATE =
         ITEMS.registerItem("settlement_mandate", properties -> new SettlementMandateItem(properties.stacksTo(1)));

   public static final DeferredItem<Item> COIN =
         ITEMS.registerItem("coin", properties -> new CurrencyItem(properties.stacksTo(64), 1));

   public static final DeferredItem<Item> COIN_STACK =
         ITEMS.registerItem("coin_stack", properties -> new CurrencyItem(properties.stacksTo(64), 10));

   DeferredItem<SpawnEggItem> MY_ENTITY_SPAWN_EGG =
         ITEMS.registerItem(
               "my_entity_spawn_egg",
               properties -> new SpawnEggItem(
                     // The entity type to spawn.
                     EntityRegistry.CIVILIZED_VILLAGER.get(),
                     // The properties passed into the lambda, with any additional setup.
                     properties));

   private static @NotNull DeferredItem<Item> registerBuildingDeedItem(BuildingType buildingType) {
      return ITEMS.registerItem(
            buildingType.name(),
            properties -> new BuildingDeedItem(properties.stacksTo(1), buildingType));
   }
}
