package com.uncreated.civilized.entity.stats;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.Map;

import com.uncreated.civilized.core.villagerinfo.Gender;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public class SkinTextureRegistry {

   public static final ResourceLocation FALLBACK =
         ResourceLocation
               .fromNamespaceAndPath(CIVILIZED_MOD_ID, "textures/entity/civilized_villager/skins/default/male/1.png");

   private static final String ROOT_PREFIX = "textures/entity/civilized_villager/skins";
   public static final String DEFAULT_CULTURE = "default";

   private static final Object2ObjectArrayMap<String, Int2ObjectMap<ResourceLocation>> CLOTHING_TEXTURES =
         Util.make(new Object2ObjectArrayMap<>(), map -> {
            add(map, DEFAULT_CULTURE, "male", "1");
            add(map, DEFAULT_CULTURE, "male", "2");
            add(map, DEFAULT_CULTURE, "male", "3");
            add(map, DEFAULT_CULTURE, "female", "1");
            add(map, DEFAULT_CULTURE, "female", "2");
            add(map, DEFAULT_CULTURE, "female", "3");
         });

   private static void add(
         Object2ObjectArrayMap<String, Int2ObjectMap<ResourceLocation>> map,
         String cultureName,
         String gender,
         String textureFileName) {
      String key = getResourceKey(cultureName, gender);
      Int2ObjectMap<ResourceLocation> selection = map.getOrDefault(key, new Int2ObjectArrayMap<>());
      selection.put(selection.size(), getResourceLocation(key, textureFileName));
      map.put(key, selection);
   }

   private static String getResourceKey(String cultureName, String gender) {
      return String.format("%s/%s/%s", ROOT_PREFIX, cultureName, gender);
   }

   private static ResourceLocation getResourceLocation(String resourceKey, String textureFileName) {
      return ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, resourceKey + "/" + textureFileName + ".png");
   }

   public static Map.Entry<Integer, ResourceLocation> getRandomSkin(
         RandomSource random,
         String cultureName,
         Gender gender) {
      String resourceKey = getResourceKey(cultureName, gender.name().toLowerCase());
      Int2ObjectMap<ResourceLocation> selection = CLOTHING_TEXTURES.get(resourceKey);
      if (selection == null)
         return Map.entry(0, FALLBACK);

      int index = random.nextInt(selection.size());
      ResourceLocation result = selection.get(index);
      if (result == null)
         return Map.entry(index, FALLBACK);

      return Map.entry(index, result);
   }
}
