package com.uncreated.civilized.entity.stats;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.villagerinfo.Gender;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;

/**
 * The villager clothing textures, found by looking through the resources for anything under
 * {@code textures/entity/civilized_villager/clothing/<culture>/<clothingSet>/<gender>/}. Only clients have textures, so
 * this is only filled in on the client, when resources are loaded or reloaded (e.g. with F3+T). Resource packs can add
 * outfits just by adding files there.
 */
public class ClothingTextureRegistry {

   private static final Logger LOGGER = LogUtils.getLogger();

   private static final String ROOT = "textures/entity/civilized_villager/clothing";
   public static final String DEFAULT_CULTURE = "default";
   public static final String DEFAULT_CLOTHING_SET = "peasant";

   public static final ResourceLocation FALLBACK =
         ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, ROOT + "/default/peasant/male/1.png");

   private static Map<String, Culture> cultures = Map.of();

   public static void reload(ResourceManager resources) {
      Map<String, Culture> found = new HashMap<>();

      // sorted, so that every client lists the textures in the same order, and picks the same one for a villager
      List<ResourceLocation> textures =
            resources.listResources(ROOT, location -> location.getPath().endsWith(".png"))
                  .keySet()
                  .stream()
                  .filter(location -> location.getNamespace().equals(CIVILIZED_MOD_ID))
                  .sorted(Comparator.comparing(ResourceLocation::getPath))
                  .toList();

      for (ResourceLocation texture : textures) {
         // <culture>/<clothingSet>/<gender>/<file>.png
         String[] parts = texture.getPath().substring(ROOT.length() + 1).split("/");
         if (parts.length != 4) {
            LOGGER.warn(
                  "Villager clothing texture {} is not in the right folder format <culture>/<set>/<gender>. It will not be loaded.",
                  texture);
            continue;
         }

         Gender gender;
         try {
            gender = Gender.valueOf(parts[2].toUpperCase());
         } catch (IllegalArgumentException e) {
            LOGGER.warn(
                  "Villager clothing texture {} not in a 'male' or 'female' subfolder. It will not be loaded.",
                  texture);
            continue;
         }

         String cultureName = parts[0];
         String clothingSetName = parts[1];
         Culture culture = found.computeIfAbsent(cultureName, Culture::new);
         ClothingSet clothingSet = culture.clothingSets().getOrCreate(clothingSetName);
         clothingSet.add(gender, texture);
      }

      cultures = found;
      LOGGER.info("Loaded {} villager clothing textures", textures.size());
   }

   public static Optional<Culture> findCulture(String name) {
      return Optional.ofNullable(cultures.get(name));
   }

   /**
    * A random outfit from any of the allowed clothing sets. Every outfit is equally likely, so sets with more outfits
    * are picked more often.
    */
   public static ResourceLocation getRandomClothingTexture(
         RandomSource random,
         String cultureName,
         List<String> allowedClothingSets,
         Gender gender) {
      Optional<Culture> culture = findCulture(cultureName);
      if (culture.isEmpty())
         culture = findCulture(DEFAULT_CULTURE);
      if (culture.isEmpty())
         return FALLBACK;

      List<ResourceLocation> options = new ArrayList<>();
      for (String clothingSet : allowedClothingSets)
         culture.get().clothingSets().find(clothingSet).ifPresent(set -> options.addAll(set.forGender(gender)));

      if (options.isEmpty())
         culture.get()
               .clothingSets()
               .find(DEFAULT_CLOTHING_SET)
               .ifPresent(set -> options.addAll(set.forGender(gender)));
      if (options.isEmpty())
         return FALLBACK;

      return options.get(random.nextInt(options.size()));
   }
}
