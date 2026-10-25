package com.uncreated.civilized.neoforge.registration.attachments;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import com.uncreated.civilized.item.quality.ItemQuality;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MyDataComponents {

   // In another class
   // The specialized DeferredRegister.DataComponents simplifies data component registration and avoids some generic
   // inference issues with the `DataComponentType.Builder` within a `Supplier`
   public static final DeferredRegister.DataComponents COMPONENTS =
         DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CIVILIZED_MOD_ID);

   public static final DeferredHolder<DataComponentType<?>, DataComponentType<CurrencyValue>> CURRENCY_VALUE =
         COMPONENTS.registerComponentType(
               "currency_value",
               builder -> builder
                     // The codec to read/write the data to disk
                     .persistent(CurrencyValue.CODEC)
                     // The codec to read/write the data across the network
                     .networkSynchronized(CurrencyValue.STREAM_CODEC));

   public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemQuality>> ITEM_QUALITY =
         COMPONENTS.registerComponentType(
               "item_quality",
               builder -> builder.persistent(ItemQuality.CODEC).networkSynchronized(ItemQuality.STREAM_CODEC));
}
