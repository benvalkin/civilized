package com.uncreated.civilized.neoforge.registration.creativetab;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import com.uncreated.civilized.neoforge.registration.ItemRegistry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CreativeTab {

   // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "civilized" namespace
   public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
         DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CIVILIZED_MOD_ID);

   // Creates a creative tab with the id "civilized:example_tab" for the example item, that is placed after the combat
   // tab
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB =
         CREATIVE_MODE_TABS.register(
               "example_tab",
               () -> CreativeModeTab.builder()
                     .title(Component.translatable("itemGroup.civilized")) // The language key for the title of your
                                                                           // CreativeModeTab
                     .withTabsBefore(CreativeModeTabs.COMBAT)
                     .icon(() -> ItemRegistry.SETTLEMENT_MANDATE.get().getDefaultInstance())
                     .displayItems((parameters, output) -> {
                        output.accept(ItemRegistry.SETTLEMENT_MANDATE.get());
                        output.accept(ItemRegistry.COIN.get());
                        output.accept(ItemRegistry.COIN_STACK.get());
                        for (DeferredItem<Item> buildingDeed : ItemRegistry.BUILDING_DEEDS.values()) {
                           output.accept(buildingDeed.get());
                        }
                     })
                     .build());
}
