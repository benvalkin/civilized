package com.uncreated.civilized.core.building.placement;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.settlement.Settlement;

import net.minecraft.network.chat.Component;

/**
 * Whether a building can go where a player wants to put it, before its requirements are even considered.
 */
public sealed interface PlacementResult {

   /**
    * @param settlement
    *           the settlement the building belongs to, or null for a new town hall with no settlement yet.
    */
   record Success(@Nullable Settlement settlement) implements PlacementResult {
   }

   /**
    * @param reason
    *           the reason message indicating why the bounds are wrong
    */
   record Failure(Component reason) implements PlacementResult {
   }
}
