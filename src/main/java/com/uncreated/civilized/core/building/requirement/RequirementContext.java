package com.uncreated.civilized.core.building.requirement;

import com.uncreated.civilized.core.building.bounds.BuildingBounds;

import net.minecraft.world.level.Level;

/**
 * Everything requirements are checked against.
 *
 * @param bounds
 *           the bounds being checked, which may not belong to a building yet, e.g. while placing one
 * @param population
 *           how many citizens the settlement has.
 * @param availableCurrency
 *           how many coins the settlement has to spend on. Typically the combined value of the player's inventory,
 *           townhall and storehouse chests.
 * @param townHallLevel
 *           the settlement's town hall level, or 0 if it doesn't have one
 */
public record RequirementContext(Level level, BuildingBounds bounds, int population, int availableCurrency,
      int townHallLevel) {
}
