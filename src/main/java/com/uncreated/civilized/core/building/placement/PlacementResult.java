package com.uncreated.civilized.core.building.placement;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.settlement.Settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Whether new building bounds proposed by the client player are valid, before its requirements are even considered.
 */
public sealed interface PlacementResult {

   /**
    * This result informs the player that their proposed bounds are invalid.
    * 
    * @param reason
    *           the reason message indicating why the bounds are wrong
    * @param highlight
    *           a problematic block to highlight to the player with visible particles
    */
   record Failure(Component reason, @Nullable BlockPos highlight) implements PlacementResult {

      public Failure(Component reason) {
         this(reason, null);
      }
   }

   /**
    * This result confirms that the player's proposed building bounds are confirmed, and the next step (usually opening
    * the building requirements GUI) will commence.
    * 
    * @param settlement
    *           the settlement the building belongs to, or null for a new town hall with no settlement yet.
    * @param bounds
    *           the building's bounds, e.g. as detected from its rooms
    * @param confirmed
    *           if this is true, this result will proceed to the next step (e.g. opening the building requirements GUI),
    *           otherwise it will display an "accepted" chat message.
    */
   record Confirmed(@Nullable Settlement settlement, BuildingBounds bounds,
         boolean confirmed) implements PlacementResult {
   }
}
