package com.uncreated.civilized.core.building.requirement;

public enum RequirementCheckPurpose {
   /** Placing a new building. All requirements need to checked. */
   ESTABLISH,
   /** Upgrading a building. All requirements for the next level need to checked. */
   UPGRADE,
   /** Redrawing a building's bounds. Usually, only structural requirements need to be checked. */
   REDRAW
}
