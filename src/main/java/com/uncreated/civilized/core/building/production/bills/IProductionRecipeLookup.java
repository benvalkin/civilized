package com.uncreated.civilized.core.building.production.bills;

import java.util.List;
import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Finds the recipe that a production bill's input items make. Server only, as recipes only exists on the server.
 */
@FunctionalInterface
public interface IProductionRecipeLookup {

   Optional<ProductionRecipe> find(List<ItemStack> inputs, ServerLevel level);
}
