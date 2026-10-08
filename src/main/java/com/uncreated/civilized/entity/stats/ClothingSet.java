package com.uncreated.civilized.entity.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.uncreated.civilized.core.villagerinfo.Gender;

import net.minecraft.resources.ResourceLocation;

/** A collection of clothing textures. Separated by gender. */
public class ClothingSet {

   private final Map<Gender, List<ResourceLocation>> textures = new EnumMap<>(Gender.class);

   void add(Gender gender, ResourceLocation texture) {
      textures.computeIfAbsent(gender, g -> new ArrayList<>()).add(texture);
   }

   public List<ResourceLocation> forGender(Gender gender) {
      return Collections.unmodifiableList(textures.getOrDefault(gender, List.of()));
   }
}
