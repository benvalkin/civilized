package com.uncreated.civilized.entity.stats;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** A keyed collection of {@link ClothingSet}s. */
public class ClothingSets {

   private final Map<String, ClothingSet> sets = new HashMap<>();

   ClothingSet getOrCreate(String clothingSet) {
      return sets.computeIfAbsent(clothingSet, s -> new ClothingSet());
   }

   public Optional<ClothingSet> find(String clothingSet) {
      return Optional.ofNullable(sets.get(clothingSet));
   }

   public Set<String> names() {
      return Collections.unmodifiableSet(sets.keySet());
   }
}
