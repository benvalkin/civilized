package com.uncreated.civilized.core.building.requirement;

public enum RequirementKind {
   /**
    * For requirements based on blocks and features that are inside the building's bounds, e.g. enough space, 2+ chests
    * present, 1x crafting table present.
    */
   STRUCTURAL,
   /** For requirements based on certain features in the rest of the settlement e.g. population, town hall level. */
   PREREQUISITE,
   /**
    * For requirements that consume a once-off sum of items. When re-drawing bounds, this type of requirement does not
    * need to be checked again.
    */
   COST
}
