package com.uncreated.civilized.core.building.production.bills;

import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.production.bills.strategy.IProductionStrategy;
import com.uncreated.civilized.core.building.production.bills.strategy.ProduceInfinite;
import com.uncreated.civilized.core.building.production.bills.strategy.ProduceUpTo;
import com.uncreated.civilized.core.building.production.bills.strategy.ProductionStrategyType;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

@Getter
public class ProductionBill {

   private final String minecraftRecipeName;
   private final ProductionType productionType;
   private final IProductionStrategy productionStrategy;
   private final int billAmount;
   @Setter
   private boolean enabled;

   private final List<ItemStack> inputItems;
   private final ItemStack displayItem;

   private final int startingProductionTokens = DEFAULT_STARTING_PRODUCTION_TOKENS;

   public static final int DEFAULT_STARTING_PRODUCTION_TOKENS = 8;

   public ProductionBill(
         String recipeName,
         ProductionType productionType,
         ProductionStrategyType productionStrategyType,
         int billAmount,
         boolean enabled,
         List<ItemStack> inputItems,
         ItemStack displayItem) {
      this.minecraftRecipeName = recipeName;
      this.productionType = productionType;
      this.billAmount = billAmount;
      this.enabled = enabled;
      this.inputItems = inputItems;
      this.displayItem = displayItem;
      this.productionStrategy = switch (productionStrategyType) {
      case ProductionStrategyType.PRODUCE_INFINITE -> new ProduceInfinite(8);
      case ProductionStrategyType.PRODUCE_UP_TO -> new ProduceUpTo(billAmount, 8);
      };
   }

   public Optional<RecipeHolder<?>> resolveRecipe(RecipeManager recipeManager) {

      ResourceKey<Recipe<?>> recipeKey =
            ResourceKey.create(Registries.RECIPE, ResourceLocation.parse(minecraftRecipeName));

      return recipeManager.byKey(recipeKey);
   }

   @Override
   public String toString() {
      return switch (productionStrategy.getType()) {
      case ProductionStrategyType.PRODUCE_INFINITE -> String.format("%s:infinite [%s, enabled:%s]", minecraftRecipeName, productionType, enabled);
      case ProductionStrategyType.PRODUCE_UP_TO ->
         String.format("%s:produce_up_to(%s) [%s, %s]", minecraftRecipeName, billAmount, productionType, enabled);
      };
   }
}
