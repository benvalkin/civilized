package com.uncreated.civilized.core.trading;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;

public class MerchantTypes {

   public static final ResourceKey<Registry<MerchantType>> REGISTRY_KEY =
         ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "merchant_type"));

   public static ResourceKey<MerchantType> key(String name) {
      return ResourceKey.create(REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, name));
   }

   public static Optional<Holder.Reference<MerchantType>> pickRandom(RegistryAccess registries, RandomSource random) {
      List<WeightedEntry.Wrapper<Holder.Reference<MerchantType>>> types =
            registries.lookupOrThrow(REGISTRY_KEY)
                  .listElements()
                  .map(type -> WeightedEntry.wrap(type, type.value().weight()))
                  .toList();

      return WeightedRandom.getRandomItem(random, types).map(WeightedEntry.Wrapper::data);
   }
}
