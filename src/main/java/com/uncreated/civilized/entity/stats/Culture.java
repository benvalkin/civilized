package com.uncreated.civilized.entity.stats;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The collection of texture resources belonging to the same culture. Currently intended for clothing textures but
 * should probably be expanded for skins/hair too.
 */
@Getter
@Accessors(fluent = true)
public class Culture {

   private final String name;
   private final ClothingSets clothingSets = new ClothingSets();

   Culture(String name) {
      this.name = name;
   }
}
